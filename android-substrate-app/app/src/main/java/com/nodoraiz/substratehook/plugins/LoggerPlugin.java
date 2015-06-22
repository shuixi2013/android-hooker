package com.nodoraiz.substratehook.plugins;

import android.util.Log;
import com.saurik.substrate.MS;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class LoggerPlugin {

    /**
     * The next variable has to contain all classes which will be hooked separated by comma symbol
     *  NOTE: initially it hasn't any value, but the JAR app will overwrite it before compile
     */
    private final static String[] CLASSES_TO_HOOK = {
"org.proxydroid.AppManager,org.proxydroid.AppManager$1,org.proxydroid.AppManager$1$1,org.proxydroid.AppManager$2,org.proxydroid.AppManager$3,org.proxydroid.AppManager$4,org.proxydroid.AppManager$ListEntry,org.proxydroid.BuildConfig,org.proxydroid.BypassListActivity,org.proxydroid.BypassListActivity$1,org.proxydroid.BypassListActivity$10,org.proxydroid.BypassListActivity$11,org.proxydroid.BypassListActivity$11$1,org.proxydroid.BypassListActivity$12,org.proxydroid.BypassListActivity$13,org.proxydroid.BypassListActivity$14,org.proxydroid.BypassListActivity$2,org.proxydroid.BypassListActivity$3,org.proxydroid.BypassListActivity$4,org.proxydroid.BypassListActivity$5,org.proxydroid.BypassListActivity$6,org.proxydroid.BypassListActivity$7,org.proxydroid.BypassListActivity$7$1,org.proxydroid.BypassListActivity$8,org.proxydroid.BypassListActivity$9,org.proxydroid.ConnectivityBroadcastReceiver,org.proxydroid.ConnectivityBroadcastReceiver$1,org.proxydroid.DNSProxy,org.proxydroid.DNSProxy$1,org.proxydroid.DomainValidator,org.proxydroid.Exec,org.proxydroid.FileArrayAdapter,org.proxydroid.FileChooser,org.proxydroid.InnerSocketBuilder,org.proxydroid.Profile,org.proxydroid.Profile$JSONDecoder,org.proxydroid.ProxyDroid,org.proxydroid.ProxyDroid$1,org.proxydroid.ProxyDroid$10,org.proxydroid.ProxyDroid$11,org.proxydroid.ProxyDroid$12,org.proxydroid.ProxyDroid$2,org.proxydroid.ProxyDroid$3,org.proxydroid.ProxyDroid$4,org.proxydroid.ProxyDroid$5,org.proxydroid.ProxyDroid$6,org.proxydroid.ProxyDroid$7,org.proxydroid.ProxyDroid$8,org.proxydroid.ProxyDroid$9,org.proxydroid.ProxyDroidReceiver,org.proxydroid.ProxyDroidService,",
"org.proxydroid.ProxyDroidService$1,org.proxydroid.ProxyDroidService$2,org.proxydroid.ProxyDroidService$3,org.proxydroid.ProxyDroidWidgetProvider,org.proxydroid.ProxyedApp,org.proxydroid.R,org.proxydroid.R$array,org.proxydroid.R$attr,org.proxydroid.R$bool,org.proxydroid.R$color,org.proxydroid.R$dimen,org.proxydroid.R$drawable,org.proxydroid.R$id,org.proxydroid.R$integer,org.proxydroid.R$layout,org.proxydroid.R$string,org.proxydroid.R$style,org.proxydroid.R$styleable,org.proxydroid.R$xml,org.proxydroid.db.DNSResponse,org.proxydroid.db.DatabaseHelper,org.proxydroid.utils.Base64,org.proxydroid.utils.Base64$1,org.proxydroid.utils.Base64$InputStream,org.proxydroid.utils.Base64$OutputStream,org.proxydroid.utils.Constraints,org.proxydroid.utils.ImageLoader,org.proxydroid.utils.ImageLoader$BitmapDisplayer,org.proxydroid.utils.ImageLoader$PhotoToLoad,org.proxydroid.utils.ImageLoader$PhotosLoader,org.proxydroid.utils.ImageLoader$PhotosQueue,org.proxydroid.utils.ImageLoaderFactory,org.proxydroid.utils.Option,org.proxydroid.utils.RegexValidator,org.proxydroid.utils.Utils,org.proxydroid.utils.Utils$ScriptRunner,"
	};
    private final static String HOOK_LOG_BREADCRUMB = "HOOK_LOG_BREADCRUMB";
    private final static String PARAMETERS_SEPARATOR = "##P_S##";

    public void apply(){

        try {
            for(String clazzBunch : CLASSES_TO_HOOK) {

                String[] classesToHook = clazzBunch.split(",");

                for (final String className : classesToHook) {

                    if(className.startsWith("android.")) {
                        Log.d(this.getClass().getName(), "REJECTED CLASS -" + className + "-");

                    } else{

                        Log.d(this.getClass().getName(), "LETS HOOK -" + className + "-");

                        MS.hookClassLoad(className, new MS.ClassLoadHook() {

                            public void classLoaded(Class<?> hookedClass) {

                                Log.d(this.getClass().getName(), "LOCATED CLASS: " + className);

                                for (final Method method : hookedClass.getDeclaredMethods()) {

                                    String parameters = "";
                                    for (Class parameterClazz : method.getParameterTypes()) {
                                        parameters += parameterClazz.getName() + ", ";
                                    }
                                    parameters = parameters.substring(0, parameters.length() - 2);
                                    final String methodSignature = className + "." + method.getName() + "(" + parameters + ")";

                                    if (Modifier.isAbstract(method.getModifiers())) {
                                        Log.d(this.getClass().getName(), "IGNORED ABSTRACT METHOD: " + methodSignature);

                                    } else {

                                        Log.d(this.getClass().getName(), "HOOKED METHOD: " + methodSignature);
                                        MS.hookMethod(hookedClass, method, new MS.MethodAlteration() {

                                            public Object invoked(Object capturedInstance, Object... args) throws Throwable {

                                                StringBuffer stringBuffer = new StringBuffer();
                                                for(int i=0;i<args.length;i++){
                                                    stringBuffer.append(args[i] + ", ");
                                                }

                                                Log.i(HOOK_LOG_BREADCRUMB, "ENTER " + methodSignature + PARAMETERS_SEPARATOR + stringBuffer.substring(0, stringBuffer.length() - 2));
                                                Object result = this.invoke(capturedInstance, args);
                                                Log.i(HOOK_LOG_BREADCRUMB, "EXIT " + methodSignature);
                                                return result;
                                            }

                                        });

                                    }

                                }
                            }
                        });
                    }
                }
            }

        } catch (Throwable e) {
            Log.d("SubstratePlugin", "Error caught: " + e.getMessage());
        }

    }
}
