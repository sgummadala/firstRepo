package com.cvs.pos;

public class MyClass {
	int age ;
	public MyClass(String name) {
		System.out.println("Name entered :" + name);
	}
	
	private void MyClassMethod(String string) {
		// TODO Auto-generated method stub
		System.out.println("Name entered :" + string);
	}

	public void setAge(int agein) {
		age = agein ; 
	}
	
	public int getAge() {
		System.out.println(" Age is equal :" + age);
		return age;
	}
	
	public static void main(String args[]) {
		//MyClass obj  = new MyClass("IndoAmerica");
		MyClass obj  = new MyClass("abc");
		obj.MyClassMethod("1America");
		//obj.MyClass("2America");
		obj.setAge(12);
		obj.getAge();
	}

	
}
