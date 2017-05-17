package backend;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author linoe
 */
public class handleBoundries extends Thread{
    interactions inter;
    public handleBoundries(interactions inter){
        super();
        this.inter=inter;
    }
    
    
    public void run() {
        while(true) {
            try {
                inter.checkBoundries();
                Thread.sleep(5);
            }catch(Exception e){};
        }
    }

}
