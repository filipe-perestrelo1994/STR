/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package backend;

/**
 *
 * @author linoe
 */
public class Product {

    public Product(int x, int z, String reference, int price, int date) {
        this.x = x;
        this.z = z;
        this.reference = reference;
        this.price = price;
        this.date = date;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public String getReference() {
        return reference;
    }

    public int getPrice() {
        return price;
    }

    public int getDate() {
        return date;
    }

    

     int x;
     int z; 
     String reference;
     int price;
     int date;
    

    

    

}
