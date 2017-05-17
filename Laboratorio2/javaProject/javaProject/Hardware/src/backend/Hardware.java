package backend;

import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author linoe
 */
public class Hardware {
    static {
        System.load("C:\\str\\Win32Project1\\x64\\Debug\\hardware.dll");
    }
    
    
    native public void create_di(int port);
    native public void create_do(int port);
    native public void write_port(int port, int value);
    native public int read_port(int port);

}
