package com.cvs.pos;


import java.io.FileReader;

import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.io.IOException;

	public class TexFileReading {
	 
	    public static void main(String[] args) {
	        try {
	       
	        	//FileReader reader = new FileReader("C:/Java/Data/MyFile.txt") ;
	        	
	        	// FileInputStream inputStream = new FileInputStream("C:/Java/Data/MyFile.txt");
	            // InputStreamReader reader = new InputStreamReader(inputStream);
	        	
	        	//or 
	        	
	        	InputStreamReader reader = new InputStreamReader(new FileInputStream("C:/Java/Data/MyFile.txt"), "UTF-8");
	        	
	            int character;
	 
	            while ((character = reader.read()) != -1) {
	                System.out.print((char) character);
	            }
	            reader.close();
	 
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    }
	 
	}
	
	