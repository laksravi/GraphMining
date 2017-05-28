import java.util.ArrayList;

class X{
	int someValue;
	X(int s){
		someValue = s;
	}
	
	@Override
	public int hashCode() {
		return someValue;
	}
	
	@Override
	public boolean equals(Object arg0) {
		return this.someValue== ((X)arg0).someValue;
	}
}

public class Test {

	public static void equality(){
		ArrayList<X[]> a = new ArrayList<X[]>();
		X[] a1= new X[2];
		a1[0] = new X(1); a1[1] = new X(2);
		a.add(a1);
		
		ArrayList<X[]> b = new ArrayList<X[]>();
		X[] a2= new X[2];
		a2[0] = new X(1); a2[1] = new X(2);
		b.add(a2);

		System.out.println(a.equals(b));
		
	}
	
	
	
	public static void main(String[] args){
		equality();
	}
	
}
