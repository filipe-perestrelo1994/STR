package backend;


import GUI.mainGUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.*;


/*
o elevador nao pode mexer enquanto estiver dentro de uma celula, falta terminar     



*/
/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author linoe
 */



public class interactions {
    public boolean debug = false;
    public boolean GUImode = false;
    public boolean isCalibrated = true;

    
    List<Product> prod = new LinkedList<>();
    
    Hardware hard;
    mainGUI _GUI;
    Semaphore sem_hard = new Semaphore(1);
    Semaphore sem_moving_x = new Semaphore(0);
    Semaphore sem_moving_y = new Semaphore(0);
    
    Mailbox worker_mbx = new Mailbox(20);
    
    boolean emergencyStop = false;
    
    void log(String message){
        if(!GUImode){
            System.out.println(message);
            
        } else {
            _GUI.log(message);//add visual log here
        }
    }
    
    boolean getBit(int value, int n_bit){
        //if value is greatter than zero
        
        return (value & (1 << n_bit)) > 0;
        
    }
    
    int setBit(int variable, int n_bit, boolean value){
        if(value){
            return variable |= (1<<n_bit); //mask on
        } else {
            return variable &= ~(1<<n_bit);  //maskoff
        }
    }
    
    int safe_read_port(int port){
        int port_value=0;
        try {
            sem_hard.acquire();
            port_value = hard.read_port(port);
            sem_hard.release();
        } catch (InterruptedException ex) {
            Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
        }
        return port_value;
        
    }
    
    void safe_write_port(int port, int value){
        try {
            sem_hard.acquire();
            hard.write_port(port, value);
            sem_hard.release();
        } catch (InterruptedException ex) {
            Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

    public void move_x_left(){
        
        Integer port_2 = safe_read_port(2);
        port_2 = setBit(port_2, 7, false);
        port_2 = setBit(port_2, 6, true);
        safe_write_port(2, port_2);
        
    }
    
    public void move_x_right(){
        
        Integer port_2 = safe_read_port(2);
        port_2 = setBit(port_2, 7, true);
        port_2 = setBit(port_2, 6, false);
        safe_write_port(2, port_2);
    }
    
    public void move_y_inside(){
        Integer port_2 = safe_read_port(2);
        port_2 = setBit(port_2, 4, false);
        port_2 = setBit(port_2, 5, true);
        safe_write_port(2, port_2);
    }
    
    public void move_y_outside(){
        Integer port_2 = safe_read_port(2);
        port_2 = setBit(port_2, 4, true);
        port_2 = setBit(port_2, 5, false);
        safe_write_port(2, port_2);
    }
    
    public void move_z_up(){
        
        Integer port_2 = safe_read_port(2);
        port_2 = setBit(port_2, 3, true);
        port_2 = setBit(port_2, 2, false);
        safe_write_port(2, port_2);
    }
    
    public void move_z_down(){
        
        Integer port_2 = safe_read_port(2);
        port_2 = setBit(port_2, 2, true);
        port_2 = setBit(port_2, 3, false);
        safe_write_port(2, port_2);
    }
    
    public void stop_x(){
        Integer port_2 = safe_read_port(2);
        port_2 = setBit(port_2, 7, false);
        port_2 = setBit(port_2, 6, false);
        safe_write_port(2, port_2);
    }
    public void stop_y(){
        Integer port_2 = safe_read_port(2);
        port_2 = setBit(port_2, 4, false);
        port_2 = setBit(port_2, 5, false);
        safe_write_port(2, port_2);
    }
    public void stop_z(){
        Integer port_2 = safe_read_port(2);
        port_2 = setBit(port_2, 3, false);
        port_2 = setBit(port_2, 2, false);
        safe_write_port(2, port_2);
    }
    
    
    
    public void put_piece_xz(int x, int z, Product p){
        _goto(1, 1);
        goto_y(1);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
        }
        Integer port_1 = safe_read_port(1);
        if(getBit(port_1, 4)){
            goto_y(2);
           
            _goto(x, z);
            goUpLevel();
            goto_y(3);
            goDownLevel();
            goto_y(2);
            prod.add(p);
            
        }else{
            log("Error: No piece detected");
        }
        
        
    }

    
    public void get_piece_xz(int x, int z, Product p){
        
        _goto(x, z);
        goto_y(3);
        goUpLevel();
        goto_y(2);
        goDownLevel();
        _goto(1, 1);
        goto_y(1);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
        }
        goto_y(2);
        prod.remove(p);
    }
    
    public void goto_x(int pos){
        
        
        int currPos=0;
        boolean is_moving = false;
	boolean stoped = false; //if it was stoped from emergency event
	int moving_way = -1; //1 move_left, 0 right
        
        for (int i=1;i<=3;i++){
            if(is_at_x(i)){
                currPos=i;
                break;
            }
        }
        if(debug)
            log("current x pos = " + currPos);
        
        if(currPos<pos){
            move_x_right();
            is_moving=true;
            moving_way = 0;
        }
        else if(currPos>pos){
            move_x_left();
            is_moving=true;
            moving_way = 1;
        }
        else return;
            
        while(!is_at_x(pos)){
            if (stoped && is_moving){ //is it was stoped and current state is moving...
                    switch (moving_way) //process way.. self explanatory :3
                    {
                    case 1:
                            move_x_left();
                            break;
                    case 0:
                            move_x_right();
                            break;
                    }  
            }
            
            if (emergencyStop){ //So, someone pressed the red button!
                    stoped = true; //we must set stoped to true
                    stop_x(); //stop elevator
                    while (emergencyStop) //wait for resume work
                    {
                            try {
                
                                Thread.sleep(10);

                            } catch (InterruptedException ex) {
                                Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
                            }
                    }
                    
            }
			
	} 

        if (is_moving){ //if elevator is moving
                stop_x(); //stop it
        }

    }
    
    public void goUpLevel(){
        //em que linha está?
        int currPos=0;
        for (int i=1;i<=3;i++){
            if(is_at_z(i)){
                currPos=i;
                break;
            }
        }
        switch(currPos){
            case 1: goto_z(4);
                break;
            case 2: goto_z(5);
                break;
            case 3: goto_z(6);
                break;
            default: log("Error: I don't know where the elevator is :(");
        }
        
    }
    
    public void goDownLevel(){
        //em que linha está?
        int currPos=0;
        for (int i=1;i<=6;i++){
            if(is_at_z(i)){
                currPos=i;
                break;
            }
        }
        switch(currPos){
            case 4: goto_z(1);
                break;
            case 5: goto_z(2);
                break;
            case 6: goto_z(3);
                break;
            default: log("Error: I don't know where the elevator is :(");
        }
    }
    
    public void goto_y(int pos){
        int currPos=0;
        boolean is_moving = false;
	boolean stoped = false; //if it was stoped from emergency event
	int moving_way = -1; //1 move_inside, 0 move outside
        
        for (int i=1;i<=3;i++){
            if(is_at_y(i)){
                currPos=i;
                break;
            }
        }
        if(debug)
            log("current y pos = " + currPos);
        if(currPos < pos){
            move_y_inside();
            is_moving=true;
            moving_way = 1;
        }
        else if(currPos > pos){
            move_y_outside();
            is_moving=true;
            moving_way = 0;
        } else return;
            
        while(!is_at_y(pos)){
            if (stoped && is_moving){ //is it was stoped and current state is moving...
                    switch (moving_way) //process way.. self explanatory :3
                    {
                    case 1:
                            move_y_inside();
                            break;
                    case 0:
                            move_y_outside();
                            break;
                    }  
            }
            
            if (emergencyStop){ //So, someone pressed the red button!
                    stoped = true; //we must set stoped to true
                    stop_y(); //stop elevator
                    while (emergencyStop) //wait for resume work
                    {
                            try {
                
                                Thread.sleep(10);

                            } catch (InterruptedException ex) {
                                Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
                            }
                    }
                    
            }
			
	} 

		
        if (is_moving){ //if elevator is moving
                stop_y(); //stop it
        }
    }
        
    public void goto_z(int pos){
        
        if(debug)
            log("moving to "+pos);
        
        int currPos=0;
        
        boolean is_moving = false;
	boolean stoped = false; //if it was stoped from emergency event
	int moving_way = -1; //1 move_up, 0 move_down
        
        for (int i=1;i<=6;i++){
            if(is_at_z(i)){
                currPos=i;
                break;
            }
        }
        if(debug)
            log("current z pos = " + currPos);
        if(pos < 4){
            if(currPos < pos){
                move_z_up();
                is_moving=true;
                moving_way = 1;
            }
            else if(currPos > pos){
                move_z_down();
                is_moving=true;
                moving_way = 0;
            }
            else return;
        } else {
            if(pos > 3){
                move_z_up();
                is_moving=true;
                moving_way = 1;
            }
            else {
                move_z_down();
                is_moving=true;
                moving_way = 0;
            }
        }

        while(!is_at_z(pos)){
            if (stoped && is_moving){ //is it was stoped and current state is moving...
                    switch (moving_way) //process way.. self explanatory :3
                    {
                    case 1:
                            move_z_up();
                            break;
                    case 0:
                            move_z_down();
                            break;
                    }  
            }
            
            if (emergencyStop){ //So, someone pressed the red button!
                    stoped = true; //we must set stoped to true
                    stop_z(); //stop elevator
                    while (emergencyStop) //wait for resume work
                    {
                            try {
                
                                Thread.sleep(10);

                            } catch (InterruptedException ex) {
                                Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
                            }
                    }
                    
            }
			
	} 

		
        if (is_moving){ //if elevator is moving
                stop_z(); //stop it
        }
    }
        
    
    public void _goto(int x,int z){
        (new Thread(){
            public void run(){
                goto_x(x);
                sem_moving_x.release(1);
            }
        }).start();
        (new Thread(){
            public void run(){
                goto_z(z);
                sem_moving_y.release(1);
            }
        }).start();
        try {
            sem_moving_x.acquire(1);
            sem_moving_y.acquire(1);
        } catch (InterruptedException ex) {
            Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(debug)
            System.out.println("ended!!!!!");
        
    }

    
    public boolean is_at_x(int pos){
        
        Integer port_0 = safe_read_port(0);
        switch(pos){
            case 1: if(!getBit(port_0, 2))
                    return true;
                break;
            case 2: if(!getBit(port_0, 1))
                    return true;
                break;
            case 3: if(!getBit(port_0, 0))
                    return true;
                break;
        }
        return false;
    } 
    
    public boolean is_at_y(int pos){
        
        Integer port_0 = safe_read_port(0);
        
        
        switch(pos){
            case 1: if(!getBit(port_0, 5))
                    return true;
                break;
            case 2: if(!getBit(port_0, 4))
                    return true;
                break;
            case 3: if(!getBit(port_0, 3))
                    return true;
                break;
        }
        return false;
    }
    
    public boolean is_at_z(int pos){
        
        Integer port_0 = safe_read_port(0);
        Integer port_1 = safe_read_port(1);
        
        switch(pos){
            case 1: if(!getBit(port_1, 3))
                    return true;
                break;
            case 2: if(!getBit(port_1, 1))
                    return true;
                break;
            case 3: if(!getBit(port_0, 7))
                    return true;
                break;
            /*UP DOWN LEVELS*/
            case 4://1-put
                if(!getBit(port_1, 2))
                    return true;
                break;
            case 5://2-put
                if(!getBit(port_1, 0))
                    return true;
                break;
            case 6://3-put
                if(!getBit(port_0, 6))
                    return true;
                break;
        }
        return false;
    } 
    
    public boolean is_at_cell(int x,int z){
        return false;
    }
    
    public boolean is_emergencyPressed(){
        Integer port_1 = safe_read_port(1);
        
        return getBit(port_1,5);
    }
    
    
    public boolean is_moving_left(){
        Integer port_2 = safe_read_port(2);
        
        return !getBit(port_2, 7) && getBit(port_2,6);
    }
    
    public boolean is_moving_right(){
        Integer port_2 = safe_read_port(2);
        
        return getBit(port_2, 7) && !getBit(port_2,6);
        
    }
    public boolean is_moving_outside(){
        Integer port_2 = safe_read_port(2);
        
        return getBit(port_2, 4) && !getBit(port_2,5);
    }
    
    public boolean is_moving_inside(){
        Integer port_2 = safe_read_port(2);
        
        return !getBit(port_2, 4) && getBit(port_2,5);
    }
     
    public boolean is_moving_down(){
        Integer port_2 = safe_read_port(2);
        
        if(getBit(port_2, 2) && !getBit(port_2,3))
            return true;
        return false;
    }
    
    public boolean is_moving_up(){
        Integer port_2 = safe_read_port(2);
        
        if(!getBit(port_2, 2) && getBit(port_2,3))
            return true;
        return false;
    }
    
    /*  calibrate_x - move until find a useful sensor.
        args: move = true - move right 
                 false - move left
        returns: nothing
    */
    public void calibrate_x(boolean move){
        if(move)
            move_x_right();
        else
            move_x_left();
        
        int i=0;
        
        while(true){
            
            try {
                if(is_at_x(1) || is_at_x(2) || is_at_x(3)){
                    break;
                }
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        stop_x();
        
    }
    
    /*  calibrate_y - move until find a useful sensor.
        args: move = true - move inside 
                 false - move outside
        returns: nothing
    */
    public void calibrate_y(boolean move){
        if(move)
            move_y_inside();
        else
            move_y_outside();
        
        int i=0;
        
        while(true){
            
            try {
                if(is_at_y(1) || is_at_y(2) || is_at_y(3)){
                    break;
                }
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        stop_y();
        
    }
    
    /*  calibrate_z - move until find a useful sensor.
        args: move = true - move up
                 false - move down
        returns: nothing
    */
    public void calibrate_z(boolean move){
        if(move)
                move_z_up();
            else
                move_z_down();

            int i=0;

            while(true){

                try {
                    if(is_at_z(1) || is_at_z(2) || is_at_z(3)){
                        break;
                    }
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
            stop_z();
            
    }
    
    /*  calibrate - Calibrate the system
        args: nothing
        returns: nothing
    */
    void calibrate(){
        int key = 0;
        
        //calibrate y
        log("We must first calibrate y axxis in order to prevent kit damage\nPress 'i' or 'o' (move inside/outside)");
        
        try {
            
            key = System.in.read(); 
            System.in.read();
        } catch (IOException ex) {
            Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
        }
        switch(key){

            case 'i':
                calibrate_y(true);
                break;
            case 'o':
                calibrate_y(false);
                break;
            
        }
        goto_y(2);
        
        //calibrate x
        log("Calibratting x axxis\nPress 'a' or 'd' (move left/right)");
        
        try {
            
            key = System.in.read(); 
            System.in.read();
        } catch (IOException ex) {
            Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
        }
        switch(key){

            case 'a':
                calibrate_x(false);
                break;
            case 'd':
                calibrate_x(true);
                break;
            
        }
        
        //calibrate z
        log("Calibratting z axxis\nPress 'w' or 'x' (move up/down)");
        try {
            key = System.in.read(); 
            System.in.read();
        } catch (IOException ex) {
            Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
        }
        switch(key){

            case 'w':
                calibrate_z(true);
                break;
            case 'x':
                calibrate_z(false);
                break;
            
        }
        
        //center the elevator
        log("Centering elevator in pos 2,2");
        _goto(2, 2); 
        
        isCalibrated = true;
        log("****Ended calibration****");
    }
   
    
    void checksafety(){
        (new Thread(){
            public void run(){
                while (true){
                    if( is_at_x(1) && is_moving_left()){
                        stop_x();
                        log("WARNING: Emergency Stop x");
                    }
                    if( is_at_x(3) && is_moving_right()) {
                        stop_x();
                        log("WARNING: Emergency Stop x");
                    }
                    if( is_at_y(1) && is_moving_outside()){
                        stop_y();
                        log("WARNING: Emergency Stop y");
                    }
                    if( is_at_y(3) && is_moving_inside()){
                        stop_y();
                        log("WARNING: Emergency Stop y");
                    }
                    if( is_at_z(1) && is_moving_down()){
                        stop_z();
                        log("WARNING: Emergency Stop z");
                    } 
                    if( is_at_z(6) && is_moving_up()){
                        stop_z();
                        log("WARNING: Emergency Stop z");

                    }
                   
                    emergencyStop = is_emergencyPressed(); // check for emergency button press
                    
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(interactions.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            
        }).start();
        
    }
    
    void help(){
        log("help:");
        log("Available commands are:");
        log("\ta - move left");
        log("\td - move right");
        log("\tw - move up");
        log("\tx - move down");
        log("\td - move right");
        log("\tp - stop");
        
        log("\tgoto [x] [z]  - move to x z pos");
        
        //Rh1	Put product P in the cell (reference, date, x, z)  - specified on the keyboard
        log("\tput [reference] [date] [price] [x] [z]  - put piece with reference and date on x z pos");
        //Rh1.1 Put product P in a free cell (reference, date)  - system selects free (x, z) itself
        log("\tput [reference] [date] [price] - put piece with reference and date into a available space");
        //Rh2	Retrieve a product from the cell (x, z) – specified on the keyboard
        log("\tget [x] [z]  - get piece from x z pos");
        //Rh3	Retrieve a product from a cell given its reference, the oldest one.
        log("\tget [reference]  - get piece with reference (oldest one)");
        
        //Rh4	Consider a STOP button for emergency situations – at the left side of the warehouse
        
        //Rh5	Consider a RESUME button towards continue the working cycle – at the left side of the warehouse

        //Rh7 Show a list with the products that are stored in the warehouse
        log("\tshowProducts - show a list of stored products");
        //Rh8 Show the product stored in a given (x,y) cell
        log("\tshowProduct [x] [z] - show product stored in x z position");
        //Rh9 Find and show the (x, y) cells of a product reference; reference provided by keyboard
        log("\tshow [reference] - show product stored with reference");
        //Rh10 Calculate the total value (in Euros) of the stored products
        log("\tshowTotalPrice - show total stored products price");
                
    }
    
    void inputCommands(){
        (new Thread(){
            public void run(){
                while(true){
                    Scanner keyboardInput = new Scanner(System.in);
                    String rxMsg = keyboardInput.nextLine();
                    String[] args = rxMsg.split("\\s+");
                    
                    switch(args[0]){
                        case "help": 
                            if(args.length==1)help();
                            break;
                        case "showProducts":
                            if(args.length==1)
                                showProducts();
                            break; 
                    case "showTotalPrice":
                        if(args.length==1)
                            showTotalPrice();
                        break;
                        
                    case "showProduct":
                        if(args.length==3)
                            if(Integer.parseInt(args[1])>=1 && Integer.parseInt(args[1])<=3 && Integer.parseInt(args[2])>=1 && Integer.parseInt(args[2])<=3)
                                showProduct_xz(Integer.parseInt(args[1]),Integer.parseInt(args[2]));
                        break; 
                        
                    case "show":
                        if(args.length==2)
                            
                                showProduct_reference(args[1]);
                        break;
                        default: worker_mbx.put(rxMsg);
                    }
                    
                    
                }      
            }
        }).start();
    }
    
    void interpreter(){
        (new Thread(){
            public void run(){
                while(true){
                
                String rxMsg = (String) worker_mbx.get();
                
                //now we need to split the string by space token in order to receive the args
                
                String[] args = rxMsg.split("\\s+");
                
                if(args.length>0)
                switch(args[0]){
                    
                    case "help": 
                        if(args.length==1)help();
                        break;
                    
                        
                    case "goto":
                        if(args.length==3)
                            if(Integer.parseInt(args[1])>=1 && Integer.parseInt(args[1])<=3 && Integer.parseInt(args[2])>=1 && Integer.parseInt(args[2])<=3)
                                _goto(Integer.parseInt(args[1]),Integer.parseInt(args[2]));
                        break;
                    //Rh1 Put product P in the cell (reference, date, price,  x, z)  - specified on the keyboard
                    case "put":
                        if(args.length==6){
                            
                            if(Integer.parseInt(args[4])>=1 && Integer.parseInt(args[4])<=3 && Integer.parseInt(args[5])>=1 && Integer.parseInt(args[5])<=3){
                                //is slot available?
                                if(isSlotEmpty(Integer.parseInt(args[4]),Integer.parseInt(args[5]))){
                                        Product p = new Product(Integer.parseInt(args[4]), Integer.parseInt(args[5]), args[1], Integer.parseInt(args[3]), Integer.parseInt(args[2]));
                                        
                                    put_piece_xz(Integer.parseInt(args[4]),Integer.parseInt(args[5]), p);
                                } else {
                                    log("already occupied");
                                }
                                //x y ref price date
                                
                            }
                        } else if(args.length == 4){ //Rh1.1 Put product P in a free cell (reference, date)  - system selects free (x, z) itself

                            int[] val = findSlot();
                            if(val[0] >0 && val[1]>0){
                                Product p = new Product(val[0], val[1], args[1], Integer.parseInt(args[3]), Integer.parseInt(args[2]));
                                
                                put_piece_xz(val[0],val[1], p);
                            }
                                
   
                        }
                        
                        break;
                        
                    
                    case "get":
                        if(args.length==3){
                            if(Integer.parseInt(args[1])>=1 && Integer.parseInt(args[1])<=3 && Integer.parseInt(args[2])>=1 && Integer.parseInt(args[2])<=3){
                                //prod iterate
                                if(!isSlotEmpty(Integer.parseInt(args[1]),Integer.parseInt(args[2]))){
                                    Iterator<Product> ProductIterator = prod.iterator();
                                    while (ProductIterator.hasNext()) {
                                            Product p = ProductIterator.next();
                                            if(p.getX()==Integer.parseInt(args[1]) && p.getZ()==Integer.parseInt(args[2])){
                                                get_piece_xz(Integer.parseInt(args[1]),Integer.parseInt(args[2]), p);
                                                break;
                                            }
                                    }
                                    
                                } else {
                                    log("slot is empty");
                                }
                            }
                        } else if(args.length==2){
                            Iterator<Product> ProductIterator = prod.iterator();
                                    while (ProductIterator.hasNext()) {
                                            Product p = ProductIterator.next();
                                            if(p.getReference().equals(args[1])){
                                                get_piece_xz(p.getX(),p.getZ(), p);
                                                break;
                                            }
                                    }
                        }
                        break;
                    case "showProducts":
                        if(args.length==1)
                            showProducts();
                        break; 
                    case "showTotalPrice":
                        if(args.length==1)
                            showTotalPrice();
                        break;
                        
                    case "showProduct":
                        if(args.length==3)
                            if(Integer.parseInt(args[1])>=1 && Integer.parseInt(args[1])<=3 && Integer.parseInt(args[2])>=1 && Integer.parseInt(args[2])<=3)
                                showProduct_xz(Integer.parseInt(args[1]),Integer.parseInt(args[2]));
                        break; 
                        
                    case "show":
                        if(args.length==2)
                            
                                showProduct_reference(args[1]);
                        break;
                    
                    //joystick code
                    case "w": 
                        
                            move_z_up(); break;
                    case "x":
                        
                            move_z_down(); break;
                    case "a": 
                        
                            move_x_left(); break;
                    case "d":  
                        
                            move_x_right(); break;
                    case "i":  
                        
                            if(is_at_x(1)|| (is_at_x(2) || (is_at_x(3))))   // valid x position?
                                if(!is_at_y(1))    // isn’t it already inside cell?
                                    goto_y(1);
                    break;
                    case "o":
                        
                            if(is_at_x(1)|| (is_at_x(2)||(is_at_x(3))))   // valid x position?
                                if(!is_at_y(3))    // isn’t it already outside cell?
                                    goto_y(3);
                    break;
                    case "p" :
                        
                        stop_x(); stop_y(); stop_z();  break;// stop all motors
                    default:log("bad command");   
                   
                }
                
                
                }
            }
        }).start();
        
    }
    
    boolean isSlotEmpty(int x, int z){
        Iterator<Product> ProductIterator = prod.iterator();
		while (ProductIterator.hasNext()) {
                        Product p = ProductIterator.next();
                        if(p.getX()==x && p.getZ()==z){
                            return false;
                        }
		}
            return true;
    }
    
    int[] findSlot(){
        int val[] = {0,0};
        for(val[0] = 1; val[0]<=3; val[0]++){
            for(val[1]=1;val[1]<=3;val[1]++){
                if(isSlotEmpty(val[0],val[1])){
                    return val;
                }
            }
        }
        val[0]=val[1]=0;
        return val;
    }
    
    //rh7 Show a list with the products that are stored in the warehouse
    
    void showProducts(){
        System.out.println("x z ref\t\tdate\t\tprice");
        Iterator<Product> ProductIterator = prod.iterator();
		while (ProductIterator.hasNext()) {
                        Product p = ProductIterator.next();
                    log("" + p.getX() + " " + p.getZ() + " " + p.getReference() + "\t" + p.getDate() + "\t" + p.getPrice());
                    
                    
		}
                
    }
    //rh8 Show the product stored in a given (x,y) cell
    
    
    void showProduct_xz(int x, int z){
        log("x y ref\t\tdate\t\tprice");
        Iterator<Product> ProductIterator = prod.iterator();
		while (ProductIterator.hasNext()) {
                        Product p = ProductIterator.next();
                        if(p.getX()==x && p.getZ()==z){
                            System.out.println("" + p.getX() + " " + p.getZ() + " " + p.getReference() + "\t" + p.getDate() + "\t" + p.getPrice());
                            return;
                        }
                        
                            
                    
		}
            
                
    }
    
    //rh9 Find and show the (x, y) cells of a product reference; reference provided by keyboard
    void showProduct_reference(String reference){
        log("x z ref\t\tdate\t\tprice");
        Iterator<Product> ProductIterator = prod.iterator();
		while (ProductIterator.hasNext()) {
                        Product p = ProductIterator.next();
                        if(p.getReference().equals(reference)){
                            System.out.println("" + p.getX() + " " + p.getZ() + " " + p.getReference() + "\t" + p.getDate() + "\t" + p.getPrice());
                            
                        }
                        
                            
                    
		}
            
                
    }
    //rh10 Calculate the total value (in Euros) of the stored products
    void showTotalPrice(){
        int price = 0;
        Iterator<Product> ProductIterator = prod.iterator();
		while (ProductIterator.hasNext()) {
                        Product p = ProductIterator.next();
                    price += p.getPrice();
		}
                log("Total warehouse stored products price is " + price +" €");
    }
    
    

    void begin(Hardware hard, mainGUI GUI) {
        this.hard=hard;
        this._GUI=GUI;
       

        
    }
}
