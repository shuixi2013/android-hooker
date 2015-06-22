package com.nodoraiz.androidhooker.gui;

import com.nodoraiz.androidhooker.models.HookerException;
import com.nodoraiz.androidhooker.utils.AdbHandler;
import com.nodoraiz.androidhooker.utils.Basics;
import com.nodoraiz.androidhooker.utils.Configuration;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Logcat extends JDialog {

    private static class MethodInvoked{

        private final static String SPACE = "  ";

        private String signature;
        private String parameters;
        private int depth;

        public String getSignature() {
            return signature;
        }

        public String getParameters() {
            return parameters;
        }

        public int getDepth() {
            return depth;
        }

        public MethodInvoked(String signature, String parameters, int depth) {
            this.signature = signature;
            this.parameters = parameters;
            this.depth = depth;
        }

        private String getSpaces(){

            StringBuffer stringBuffer = new StringBuffer();
            for(int i=0 ; i<this.depth ; i++){
                stringBuffer.append(SPACE);
            }
            return stringBuffer.toString();
        }

        @Override
        public String toString() {
            return this.getSpaces() + this.signature;
        }
    }

    private final static String BREADCRUMB_TOKEN = "HOOK_LOG_BREADCRUMB";
    private final static String TOKEN_ENTER_METHOD = "ENTER ";
    private final static String TOKEN_EXIT_METHOD = "EXIT";
    private final static String PARAMETERS_SEPARATOR = "##P_S##";
    private final static int MAX_LOG_LINES = 1024;
    private final static int CHECK_LOGCAT_INTERVAL_IN_MILISECONDS = 2000;

    private JPanel contentPane;
    private JButton buttonOK;
    private JList listLog;
    private JTextArea textAreaParameters;
    private Timer timer;

    private LinkedList<MethodInvoked> methodInvokedList;
    private int spaces = 0;
    private boolean dumpEnabled = true;

    public Logcat() {

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        this.methodInvokedList = new LinkedList<MethodInvoked>();
        this.prepareEvents();
    }

    private void prepareEvents() {

        this.buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        this.listLog.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (Logcat.this.listLog.getSelectedValue() != null) {
                    MethodInvoked methodInvoked = (MethodInvoked) Logcat.this.listLog.getSelectedValue();
                    Logcat.this.textAreaParameters.setText(methodInvoked.getParameters());
                }
            }
        });

        this.addWindowListener(new WindowListener() {

            @Override
            public void windowClosed(WindowEvent e) {

                // disable timer before exit
                if (Logcat.this.timer != null) {
                    Logcat.this.timer.stop();
                    for (ActionListener actionListener : Logcat.this.timer.getActionListeners()) {
                        Logcat.this.timer.removeActionListener(actionListener);
                    }
                }

                // dump last invoked methods detected
                Logcat.this.dumpInvokedMethodsToFile(Logcat.this.methodInvokedList);
            }

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });

        this.timer = new Timer(Logcat.CHECK_LOGCAT_INTERVAL_IN_MILISECONDS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {

                    // look for breadcrumbs
                    for (String line : AdbHandler.readLogcat(true)) {

                        if (line.contains(BREADCRUMB_TOKEN)){

                            if(line.contains(TOKEN_ENTER_METHOD)) {

                                String[] tokens = line.substring(line.indexOf(TOKEN_ENTER_METHOD) + TOKEN_ENTER_METHOD.length()).split(PARAMETERS_SEPARATOR);
                                Logcat.this.methodInvokedList.addFirst(new MethodInvoked(tokens[0], tokens[1], spaces));
                                spaces++;

                            } else if(line.contains(TOKEN_EXIT_METHOD)){
                                if(spaces > 0) spaces--;
                            }
                        }
                    }

                    if(Logcat.this.methodInvokedList.size() > MAX_LOG_LINES){
                        Logcat.this.handleLogOverflow();
                    }

                    Logcat.this.listLog.setListData(Logcat.this.methodInvokedList.toArray(new Object[Logcat.this.methodInvokedList.size()]));

                } catch (Exception exception){
                    Basics.logError(exception);
                }
            }
        });

        this.timer.start();
    }

    private void handleLogOverflow() {

        ArrayList<MethodInvoked> exceededSublist = null;
        if(this.dumpEnabled) {
            // get exceeded sublist to dump into file
            exceededSublist = new ArrayList<MethodInvoked>(
                    this.methodInvokedList.subList(Logcat.MAX_LOG_LINES, this.methodInvokedList.size())
            );
            // inverse the order of the list, in the dump last line will be the latest method invoked detected
            Collections.reverse(exceededSublist);
        }

        // remove exceeded part from the list
        LinkedList<MethodInvoked> linkedList = new LinkedList<MethodInvoked>(this.methodInvokedList.subList(0, Logcat.MAX_LOG_LINES));
        this.methodInvokedList.clear();
        this.methodInvokedList = linkedList;

        if(this.dumpEnabled && exceededSublist != null) {
            this.dumpInvokedMethodsToFile(exceededSublist);
        }
    }

    private void dumpInvokedMethodsToFile(List<MethodInvoked> methodInvokedList){

        if(this.dumpEnabled) {
            // dump into file in inverted order, last line will be the latest method invoked detected
            StringBuffer stringBuffer = new StringBuffer();
            for (MethodInvoked methodInvoked : methodInvokedList) {
                stringBuffer.append(methodInvoked.toString() + " => " + methodInvoked.getParameters() + "\n");
            }

            try {
                Basics.writeFile(Configuration.LOGCAT_FILE.getAbsolutePath(), stringBuffer.toString(), true);

            } catch (HookerException e) {
                Basics.logError(e);
                this.dumpEnabled = false;
                JOptionPane.showMessageDialog(null, "The dump of the detected methods invoked was disabled. Reason: " + e.getMessage());
            }
        }
    }

    private void onOK() {

        dispose();
    }

    public static void showDialog(){

        Logcat dialog = new Logcat();
        dialog.pack();
        dialog.setTitle("Hooker logcat");
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
