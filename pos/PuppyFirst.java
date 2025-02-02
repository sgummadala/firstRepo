package com.cvs.pos;

public class PuppyFirst {
	int puppyAge;
	
	//Constructor similar  to method but just name of the class
	public PuppyFirst(String name) {
		System.out.println("Name of the Person :" + name);
	}	
	
	
	// Method to set the age to variable puppyAge
	public void setAge( int age ) {
		puppyAge = age;
	}
	
	//Method to to return the age
	public int getAge() {
		System.out.println("Age of the America is :" + puppyAge); 
		return puppyAge;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//Create object
		PuppyFirst myPuppy = new PuppyFirst("America");

		
		//Call to set age
		myPuppy.setAge(12);
		
		//Call to get age
		myPuppy.getAge();
		
			
	}

}
