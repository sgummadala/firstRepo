package com.cvs.pos;

public class EmpTest {

	public static void main(String[] args) 
	{
		/* Create two objects using constructor */
		Emp objEmp1 = new Emp("Rajesh");
		//Emp objEmp2 = new Emp("Suresh");
	
	// Calling Methods using objects 
	objEmp1.setAge(10);
	objEmp1.setPosition("Dev");
	objEmp1.setSalary(1000.45);
	
	//Call Method print
	objEmp1.printEmp();
	}
}

