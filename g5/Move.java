package sqdance.g5;

import sqdance.sim.Point;

public class Move {
	int id;
	int targetRow;
	Point targetPos;
	Shape targetShape;

	public Move(int tid, Point tp, Shape ts, int tr) {
		this.id = tid;
		this.targetPos = tp;
		this.targetRow = tr;
		this.targetShape = ts;
	}

	public Move(int tid, Bar bar, int row, int column) {
		this.id = tid;
		this.targetRow = row;
		this.targetShape = bar;

		// find the target position
		Point targetPos;
		if (column == 0) {
			targetPos = bar.leftColumn.get(row);
		} else if (column == 1) {
			targetPos = bar.rightColumn.get(row);
		} else {
			// System.out.println("Error: request to put dancer " + tid + " to column " + column + " of " + this);
			targetPos = null;
		}
		this.targetPos = targetPos;
	}

	public Move(int tid, Line line, int row) {
		this.id = tid;
		this.targetRow = row;
		this.targetShape = line;

		// find the target position by position
		Point target = line.spots.get(row);
		this.targetPos = target;
	}

	@Override
	public String toString() {
		return targetPos.toString();
	}
}
