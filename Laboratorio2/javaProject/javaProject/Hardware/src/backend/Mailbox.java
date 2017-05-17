/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backend;

import java.util.LinkedList;
import java.util.concurrent.Semaphore;

/**
 *
 * @author linoe
 */
public class Mailbox {
    private final java.util.concurrent.Semaphore E, S, R;
    private LinkedList box; 
    
    public Mailbox(int size) {
        E = new Semaphore(1);
        S = new Semaphore(size);
        R = new Semaphore(0);
        box = new LinkedList();
    }
    
    public void put(Object data) {
        try {
            S.acquire();  // corresponde ‘a operacao P
            E.acquire();
            box.add(data);
            E.release(); // corresponde a operacao V
            R.release();
        }catch(Exception e) { e.printStackTrace();  }        
    }
    
    public Object get() {
        Object data=null;
        try {
            R.acquire();
            E.acquire();
            data=box.getFirst(); //obter sempre a primeira mensagem da mailbox
            box.removeFirst();
            E.release();
            S.release();
        }catch(Exception e) { e.printStackTrace();  }        
        return data;
    }
}
