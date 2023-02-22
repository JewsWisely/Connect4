/*
   Author: Joel Anglister
   ~ 8 hrs of work

   What I learned: 
   The reason I started this lab in the first place was to practice making a back and forward button to undo and redo
   certain things, but I ended up learning much more. First off, I correctly implemented the undo and redo buttons, so
   I reached the goal that I set for myself through this project. However, I did some other things that I have never
   done before. For example, I made my class a subclass of JFrame instead of JPanel. Not much changed other than where
   certain code segments have to go. Second, I created my own graphics in Microsoft Paint and used them for my buttons.
   I fixed the size of the window because as I resized it, the icons I made didn't resize along with the size of the
   buttons. Then I realized that someone may have a monitor that is smaller than 699 x 641 pixels, so I used the Toolkit
   class to find out the dimensions of a user's screen. If they're bigger than 699 x 641, it's fine, but otherwise I
   halve the size of the icons and of the window. I leave the undo and redo buttons at 42 x 42 pixels. This is the first
   time I have used the setPreferredSize method. Also, I'm quite proud of myself for thinking of using an array that
   keeps track of the bottom of each column, because it allows for the user to click any button and have the piece
   "drop down" to the bottom. Also, it speeds up the find of the pressed button (in the checkForWin method) because you
   only need to check from the bottom of each column to the top, since all the other buttons are disabled and can not be
   pressed.

 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Stack;

@SuppressWarnings("serial")
public class ConnectFour extends JFrame //ConnectFour is a JFrame, not a JPanel like I usually do
{
	private byte turn; //red is 1, black is 2
	private byte[][] matrix; //blue is 0, red is 1, black is 2
	private boolean ai = false;
	private JButton[][] board;
	private JLabel turnIndicator;
	private JButton[] undoRedo = new JButton[2]; //first spot holds undo button, second holds redo button
	private final int ROWS = 6;
	private final int COLS = 7;
	private ImageIcon[] blueRedBlack = new ImageIcon[3]; /*holds the following icons in order: blue, red, black, current turn.
                                                            current turn is represented by an icon, either red or black*/
	public ConnectFour()
	{
		//create the ImageIcons from the graphics I drew, which are 100 x 100 pixels
		blueRedBlack[0] = new ImageIcon("Connect4_BLUE.png");
		blueRedBlack[1] = new ImageIcon("Connect4_RED.png");
		blueRedBlack[2] = new ImageIcon("Connect4_BLACK.png");

		//I don't allow the user to resize the window, so I ensure that the window fits the screen
		//first I get the dimensions of the screen, then check if the window 699 x 641 would fit
		//if not, then I make the squares 50 x 50 pixels and resize the window to fit all of the buttons
		Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
		int height = (int)size.getHeight(), width = (int)size.getWidth();
		System.out.println("Monitor width: " + width + ", Monitor height: " + height);
		if(width >= 699 && height >= 641)
		{
			setSize(699, 641);
			System.out.println("Game window width: " + 699 + ", Game window height: " + 641);
		}
		else
		{
			for(int i = 0; i < blueRedBlack.length; i++)
				blueRedBlack[i] = new ImageIcon(blueRedBlack[i].getImage().getScaledInstance(50, 50, Image.SCALE_FAST));
			setSize(342, 341);
			System.out.println("Game window width: " + 342 + ", Game window height: " + 341);
		}
		setResizable(false);
		setTitle("Connect Four! by Joel Anglister");
		setLocation(200, 20);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		start();
	}

	private void start()
	{
		//everything will go onto the main JPanel
		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());

		//holds the undo and redo buttons, as well as the turn indicator label. everything is put as far left as possible
		JPanel top = new JPanel();
		top.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

		//create the board and the action listener for the buttons
		board = new JButton[ROWS][COLS];
		Listener listener = new Listener();
		matrix = new byte[ROWS][COLS];

		//set the current turn to the red icon
		turn = 1;

		//create the undo and redo buttons, make them 42 x 42 pixels, and add them to the top JPanel
		for(int i = 0; i < 2; i++)
		{
			undoRedo[i] = new JButton();
			undoRedo[i].setBorderPainted(false);
			undoRedo[i].setPreferredSize(new Dimension(42, 42));
			undoRedo[i].addActionListener(listener);
			top.add(undoRedo[i]);
		}
		undoRedo[0].setIcon(new ImageIcon("Connect4_UNDO.png"));
		undoRedo[1].setIcon(new ImageIcon("Connect4_REDO.png"));

		//create the turn indicator label, set its font size to 32, and add it to the top JPanel
		turnIndicator = new JLabel("Turn: RED");
		turnIndicator.setForeground(Color.RED);
		turnIndicator.setFont(turnIndicator.getFont().deriveFont(32f));
		top.add(turnIndicator);

		//this panel will hold all the "board" or "game" buttons
		JPanel buttonHolder = new JPanel();
		buttonHolder.setLayout(new GridLayout(ROWS, COLS));

		//create each game button and add it to the button holder panel
		for(int r = 0; r < ROWS; r++)
			for(int c = 0; c < COLS; c++)
			{
				board[r][c] = new JButton();
				board[r][c].setBorderPainted(false);
				board[r][c].setIcon(blueRedBlack[0]);
				board[r][c].addActionListener(listener);
				buttonHolder.add(board[r][c]);
			}

		//put the button holder in the middle of the panel, the top panel at the top,
		//and since this is a JFrame, set this object's content pane to the main panel
		//and make it visible
		main.add(buttonHolder, BorderLayout.CENTER);
		main.add(top, BorderLayout.NORTH);
		setContentPane(main);
		setVisible(true);
	}

	//args: r = current row, c = current column
	//pre: r and c must correspond to a certain button's row and column
	//post: return whether the current situation is a win or not
	/*public boolean checkForWin(int r, int c)
	{
		if(r < 0)
			return false;
		//check 4 spaces in each direction, use inARow to keep track of 
		//how many pieces in a row are of the current turn's color
		int inARow = 0;

		//if r <= 2, a vertical win is possible, so go down 4 spaces and count how many pieces are correct
		//if all 4 are, then return true
		if(r <= 2)
		{
			for(int i = 0; i < 4; i++)
				if(matrix[r + i][c] == turn)
					inARow++;
			if(inARow == 4)
				return true;
			inARow = 0;
		} //vertical win

		//check for a horizontal win by traversing from 4 spaces to the left to 4 spaces to the right of
		//the current button. if there aren't 4 spaces to the left, then start at 0. if the end of the row
		//is reached, break out of the loop. add one to inARow for each consecutive piece colored correctly,
		//but set inARow to 0 once an incorrect piece is reached. if inARow is ever 4, then return true.
		for(int col = c - 4; col < c + 4; col++)
		{
			if(col < 0)
				col = 0;
			if(col > COLS - 1)
				break;
			if(matrix[r][col] == turn)
				inARow++;
			else
				inARow = 0;
			if(inARow == 4)
				return true;
		} //horizontal win

		//check for an up-right diagonal win:
		//create 2 new variables: row and col. row starts 4 rows further down than r, and col starts 4
		//columns further left than c. Since often times these coordinates will not fall on the board,
		//increase the column and decrease the row until they both land on the board. Then, proceed
		//with the same instructions aforementioned in the horizontal check, except increase the column
		//and decrease the row for every iteration
		inARow = 0;
		int row = r + 4, col = c - 4;
		while(row >= ROWS || col < 0)
		{
			row--;
			col++;
		}
		while(row > r - 4 && col < c + 4 && row >= 0 && col < COLS)
		{
			if(matrix[row--][col++] == turn)
				inARow++;
			else
				inARow = 0;
			if(inARow == 4)
				return true;
		} //up-right diagonal win

		//check for down-right diagonal win
		//same as up-right but row starts at r - 4 increase row instead of decreasing.
		inARow = 0;
		row = r - 4;
		col = c - 4;
		while(row < 0 || col < 0)
		{
			row++;
			col++;
		}
		while(row < r + 4 && col < c + 4 && row < ROWS && col < COLS)
		{
			if(matrix[row++][col++] == turn)
				inARow++;
			else
				inARow = 0;
			if(inARow == 4)
				return true;
		} //down-right diagonal win

		return false; //no win
	}*/

	//this listener class will be the same for every button
	//it will handle the game buttons, undo, and redo
	private class Listener implements ActionListener
	{
		//with the use of the bottom array, players are allowed to click on any empty space
		//in order to drop a piece in that column. the history and future changer keep track
		//of the order in which pieces were placed. These are used for redo and undo functions.
		//bottom is filled with 7 5s, index of the bottom of each column = 5, # columns = 7.
		private int[] bottom = {5, 5, 5, 5, 5, 5, 5};
		private Stack<JButton> history = new Stack<JButton>();
		private Stack<JButton> future = new Stack<JButton>();

		//switch the state of the label and the current turn icon
		private void correctTurnAndIndicator()
		{
			if(turn == 1)
			{
				turn = 2;
				turnIndicator.setText("Turn: BLACK");
				turnIndicator.setForeground(Color.BLACK);
			}
			else
			{
				turn = 1;
				turnIndicator.setText("Turn: RED");
				turnIndicator.setForeground(Color.RED);
			}
		}

		//args: changer = the stack whose top button we want to change
		//      holder is the stack which will hold changer's top button
		//pre: changer and holder are history and future
		//post: either go backwards one move or forwards one move
		private void changeHistory(Stack<JButton> changer, Stack<JButton> holder)
		{
			//if you either can't undo or redo, depending on if changer is history or future, return
			//otherwise, create JButton button to hold the changer's top button, pop it from changer
			//and push it onto holder
			if(changer.isEmpty())
				return;
			holder.push(changer.peek());
			JButton button = changer.pop();

			//if button is being reverted to blank, enable it and make it blue
			//otherwise (button is being reverted to a color) disable it and make it the current turn's color
			//also, search for the button, and change the bottom of that column accordingly in the array
			//finally, correct the current turn and the turn indicator, and return
			button.setDisabledIcon(changer == history ? blueRedBlack[0] : blueRedBlack[turn]);
			button.setEnabled(changer == history);
			for(int r = 0; r < ROWS; r++)
				for(int c = 0; c < COLS; c++)
					if(board[r][c] == button)
					{
						matrix[r][c] = (byte)(changer == history ? 0 : turn);
						if(changer == history)
							bottom[c]++;
						else
							bottom[c]--;
						correctTurnAndIndicator();
						return;
					}
		}

		//check for a win on the board
		//return 0 if no win, 1 if red wins, 2 if black wins.
		public byte checkForWin(byte[][] matrix) {

			//check for vertical win
			//for each consecutive number in each column, increment gucc by one
			//if it reaches 4, return the digit in the matrix
			//if the next number is different, set gucc to 0
			int gucc = 0;
			for(int c = 0; c < COLS; c++) {
				if(bottom[c] < 2)
					for(int r = bottom[c] + 1; r < ROWS; r++) 
						if(matrix[r][c] == matrix[2][c]) {
							if(++gucc == 4)
								return matrix[2][c];
						}
						else
							gucc = 0;
				gucc = 0;
			}

			//check for horizontal win
			//for a horizontal win to be possible, there must be a piece in the middle column (column 3)
			//for each row with an occupied middle, start at column 0 and increment gucc by one for each consecutive number equal to the middle
			//if gucc reaches 4, return the digit in the middle column of the row
			//if the next number is different, set gucc to 0
			gucc = 0;
			for(int r = bottom[3] + 1; r < ROWS; r++) {
				for(int c = 0; c < COLS; c++)
					if(matrix[r][c] == matrix[r][3]) {
						if(++gucc == 4)
							return matrix[r][3];
					}
					else
						gucc = 0;
				gucc = 0;
			}

			//check for diagonal win
			//a diagonal win requires that a piece be in the middle column
			//search through each diagonal that passes through the given r coordinate
			gucc = 0;
			int row, col;
			for(int r = bottom[3] + 1; r < ROWS; r++) {
				col = 3 - r < 0 ? 0 : 3 - r; //consider putting 3 - r in parentheses
				row = r - 3 < 0 ? 0 : r - 3; //consider putting r - 3 in parentheses
				//downright
				while(row < ROWS && col < COLS)
					if(matrix[row++][col++] == matrix[r][3]) {
						if(++gucc == 4)
							return matrix[r][3];
					}
					else
						gucc = 0;

				gucc = 0;

				col = r - 2 < 0 ? 0 : r - 2;
				row = r + 3 >= ROWS ? ROWS - 1 : r + 3;
				//upright
				while(row >= 0 && col < COLS)
					if(matrix[row--][col++] == matrix[r][3]) {
						if(++gucc == 4)
							return matrix[r][3];
					}
					else
						gucc = 0;

				gucc = 0;
			}

			return 0;
		}


		@Override
		public void actionPerformed(ActionEvent e)
		{
			if(e.getSource() == undoRedo[0])
			{  //undo a move
				changeHistory(history, future);
			}
			else if(e.getSource() == undoRedo[1])
			{  //redo a move
				changeHistory(future, history);
			}
			else
			{  
				//player clicked a game button. search for the clicked button's row and column by traversing
				//each column, starting at the bottom (index taken from the bottom array) and going up
				for(int c = 0; c < COLS; c++)
					for(int r = bottom[c]; r >= 0; r--)
						if(board[r][c] == e.getSource())
						{
							makeMove(c);
							return;
						}
			}
		}

		public void makeMove(int c) {
			//when a button is pressed, only the button in the bottom of the column is dealt with
			//push button onto history stack and clear future stack
			//set the button to the color it needs to be and disable it
			history.push(board[bottom[c]][c]);
			future.clear();
			board[bottom[c]][c].setDisabledIcon(blueRedBlack[turn]);
			board[bottom[c]][c].setEnabled(false);
			matrix[bottom[c]][c] = (byte)(turn);
			bottom[c]--;

			//display matrix
			/*System.out.println();
			for(int r = 0; r < ROWS; r++) {
				for(int col = 0; col < COLS; col++)
					System.out.print(matrix[r][col] + " ");
				System.out.println();
			}*/

			//if you reach the top of a column (bottom[c] is 0), then sum up the bottom array
			//it can range from -6 (-6 from 6 full columns and 0 from the current column), to
			//30 (-30 from 6 empty columns and 0 from the current column). if it's a -6, then
			//the game is tied UNLESS the game is won on the current move. For that reason, I
			//check if the sum = -6 and the game wasn't won in order to ensure that it is a tie.
			int sum = 0;
			if(bottom[c] == 0)
				for(int i = 0; i < COLS; i++)
					sum += bottom[i];
			byte won = checkForWin(matrix);

			//either a win or tie has occurred, so tell the player who won or if it was a tie,
			//then ask if he wants to play again. If yes, clear the history, clear the future,
			//restart the game, and return. Otherwise, close the game window.
			if(won != 0 || sum == -6)
			{
				int option = JOptionPane.showConfirmDialog(null, (sum == -6 && won != 0 ? "Tie!" : (turn == 1 ? "RED won!" : "BLACK won!")) + "\nPlay again?", "Play again?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if(option == JOptionPane.YES_OPTION)
				{
					start();
					history.clear();
					future.clear();
					return;
				}
				else
					System.exit(0);
			}

			//if the application is closed, this does nothing
			//if it is a normal move, it updates the turn, indicator, and bottom array
			//if the game has just restarted, then this code will not run because it
			//will have returned out of the method
			correctTurnAndIndicator();

			if(!ai)
				return;
			if(turn == 2) {
				long t1 = System.nanoTime();
				int[] scores = new int[7];
				for(int col = 0; col < COLS; col++) {
					if(bottom[col] != -1) {
						matrix[bottom[col]][col] = 2;
						scores[col] = minimax(matrix, turn == 2, 2);
						matrix[bottom[col]][col] = 0;
					}
				}
				int col = 0;
				System.out.println();
				for(int i = 0; i < scores.length; i++) {
					System.out.print(scores[i] + " ");
					if(bottom[i] != -1 && bottom[col] == -1 || scores[i] < scores[col])
						col = i;
				}
				System.out.println();
				makeMove(col);
				long t2 = System.nanoTime();
				System.out.println("ms: " + (t2 - t1) / 1000000.0);
			}
		}

		public int minimax(byte[][] matrix, boolean copmuter, int depth) {
			//if a player has won or depth is 0, return the static evaluation of this board
			if(depth == 0 || checkForWin(matrix) != 0)
				return staticEvaluation(matrix);

			int eval = copmuter ? 999 : -999;
			for(int col = 0; col < COLS; col++) {
				if(bottom[col] >= 0) {
					matrix[bottom[col]][col] = (byte)(copmuter ? 2 : 1);
					bottom[col]--;
					eval = copmuter ? Math.min(eval, minimax(matrix, !copmuter, depth - 1)) : Math.max(eval, minimax(matrix, !copmuter, depth - 1));
					bottom[col]++;
					matrix[bottom[col]][col] = 0;
				}
			}
			return eval;
		}

		public int staticEvaluation(byte[][] matrix) {

			int win = checkForWin(matrix);
			if(win != 0)
				return win == 2 ? -999 : 999;
			
			int score = 0;
			/*for(int r = 0; r < ROWS; r++)
				for(int c = 2; c <= 4; c++)
					score += matrix[r][c] == 2 ? -1 : 1;
			
			for(int c = 0; c < COLS; c++)
				for(int r = 2; r <= 3; r++)
					score += matrix[r][c] == 2 ? -1 : 1;*/
			
			return score;

		}

	}

	public static void main(String[] args)
	{
		JFrame frame = new ConnectFour();
	}
}