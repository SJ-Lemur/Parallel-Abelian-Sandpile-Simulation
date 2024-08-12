package parallelAbelianSandpile;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class Grid {
    private int rows, columns;
    private int[][] grid; // current grid
    private int[][] updateGrid; // grid for next time step
	ForkJoinPool pool = ForkJoinPool.commonPool();
    // Constructor to initialize the grid
    public Grid(int w, int h){
        rows = w + 2; // for the "sink" border
        columns = h + 2; // for the "sink" border
        grid = new int[this.rows][this.columns];
        updateGrid = new int[this.rows][this.columns];
        // Initialize grid with zeros
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.columns; j++) {
                grid[i][j] = 0;
                updateGrid[i][j] = 0;
            }
        }
    }

    // Constructor to initialize the grid from an existing array
	public Grid(int[][] newGrid) {
		this(newGrid.length,newGrid[0].length); //call constructor above
		//don't copy over sink border
		for(int i=1; i<rows-1; i++ ) {
			for( int j=1; j<columns-1; j++ ) {
				this.grid[i][j]=newGrid[i-1][j-1];
			}
		}
		
	}

    // Get the number of rows, excluding the sink border
    public int getRows() {
        return rows - 2;
    }

    // Get the number of columns, excluding the sink border
    public int getColumns() {
        return columns - 2;
    }

    // Get the value at a specific cell
    int get(int i, int j) {
        return grid[i][j];
    }

    // Set all values in the grid to a specific value, excluding the borders
    void setAll(int value) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                grid[i][j] = value;
            }
        }
    }

    // For the next timestep - copy updateGrid into grid
	public void nextTimeStep() {
		for(int i=1; i<rows-1; i++ ) {
			for( int j=1; j<columns-1; j++ ) {
				this.grid[i][j]=updateGrid[i][j];
			}
		}
	}

    // Key method to calculate the next update grid
    boolean update() {
    	
    	SandpileToppler task = new SandpileToppler(1, rows);
    	boolean change = pool.invoke(task);
        if (change) {
            nextTimeStep();
        }
        
        return change;
    }

    class SandpileToppler extends RecursiveTask<Boolean> {
        final static int SEQUENTIAL_CUTOFF = 10;
        int startRow, endRow; 

        SandpileToppler(int startRow, int endRow) {
            this.startRow = startRow;
            this.endRow = endRow;
            
            }


        @Override
        protected Boolean compute() {
            boolean change = false;
            if ((endRow - startRow) <= SEQUENTIAL_CUTOFF) { 
                if(endRow == grid.length) endRow = endRow -1;
                
			for (int i = startRow; i < endRow; i++) {
        		for (int j = 1; j < columns - 1; j++) {
            			updateGrid[i][j] = (grid[i][j] % 4) + // Performs the toppling of each cell
						(grid[i-1][j] / 4) +
						grid[i+1][j] / 4 +
						grid[i][j-1] / 4 + 
						grid[i][j+1] / 4;
			
				if (grid[i][j] != updateGrid[i][j]) {  // Monitors whether the grid has changed
					change=true;;
				 }
        		}
    		}
    	
    	}
    
            else {
            
                int midRow = (startRow + endRow) / 2; 
                SandpileToppler left = new SandpileToppler(startRow, midRow);
                SandpileToppler right = new SandpileToppler(midRow, endRow);
                left.fork();


                boolean change1 = right.compute();
                boolean change2 = left.join();
                change =  change1 || change2;
            }
            
          return change; 
    }
 }

    // Display the grid in text format
	void printGrid( ) {
		int i,j;
		//not border is not printed
		System.out.printf("Grid:\n");
		System.out.printf("+");
		for( j=1; j<columns-1; j++ ) System.out.printf("  --");
		System.out.printf("+\n");
		for( i=1; i<rows-1; i++ ) {
			System.out.printf("|");
			for( j=1; j<columns-1; j++ ) {
				if ( grid[i][j] > 0) 
					System.out.printf("%4d", grid[i][j] );
				else
					System.out.printf("    ");
			}
			System.out.printf("|\n");
		}
		System.out.printf("+");
		for( j=1; j<columns-1; j++ ) System.out.printf("  --");
		System.out.printf("+\n\n");
	}
	

    // Write grid out as an image
	void gridToImage(String fileName) throws IOException {
        BufferedImage dstImage =
                new BufferedImage(rows, columns, BufferedImage.TYPE_INT_ARGB);
        //integer values from 0 to 255.
        int a=0;
        int g=0;//green
        int b=0;//blue
        int r=0;//red

		for( int i=0; i<rows; i++ ) {
			for( int j=0; j<columns; j++ ) {
			     g=0;//green
			     b=0;//blue
			     r=0;//red

				switch (grid[i][j]) {
					case 0:
		                break;
		            case 1:
		            	g=255;
		                break;
		            case 2:
		                b=255;
		                break;
		            case 3:
		                r = 255;
		                break;
		            default:
		                break;
				
				}
		                // Set destination pixel to mean
		                // Re-assemble destination pixel.
		              int dpixel = (0xff000000)
		                		| (a << 24)
		                        | (r << 16)
		                        | (g<< 8)
		                        | b; 
		              dstImage.setRGB(i, j, dpixel); //write it out

			
			}}
		
        File dstFile = new File(fileName);
        ImageIO.write(dstImage, "png", dstFile);
	}
	

}
