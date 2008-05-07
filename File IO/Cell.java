public class Cell implements java.io.Serializable{

	private int row;
	private int col;
	
	public Cell(int r, int c){
		row = r;
		col = c;
	}

	public int getRow(){
		return row;
	}

	public int getCol(){
		return col;
	}
}