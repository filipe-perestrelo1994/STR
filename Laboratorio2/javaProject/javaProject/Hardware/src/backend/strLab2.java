package backend;


import GUI.mainGUI;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;



/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author linoe
 */

public class strLab2 {
    
    
    
     static public void main(String []args){
       
         
        mainGUI GUI = new mainGUI();
        
        Hardware hard = new Hardware();
        hard.create_di(0);
        hard.create_di(1);
	hard.create_do(2);
	
	
        interactions inter = new interactions();
        inter.begin(hard, GUI);
        
         //first ask if we go gui or not!
        System.out.println("Do you want to start in GUI mode? (y/n):");
        int key = 0;
        try {
            key = System.in.read();
            System.in.read(); // read \n
        } catch (IOException ex) {
            Logger.getLogger(strLab2.class.getName()).log(Level.SEVERE, null, ex);
        }
        inter.checksafety();
        if(key == 'y')
            inter.GUImode = true;
        
        if(!inter.GUImode){
            //calibrate system
            
            inter.calibrate();

            inter.interpreter();
            inter.inputCommands();
        } else {
            System.out.println("Starting graphical user interface...");
            
            GUI.begin(inter);
            
        }
        
        
	
        
}

}
