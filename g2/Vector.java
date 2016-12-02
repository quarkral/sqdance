package sqdance.g2;

import sqdance.sim.Point;

public class Vector {

    double x;
    double y;
    public int state;

    final double EPSILON = 1e-7;

    public Vector( double x, double y){
        this.x = x;
        this.y = y;
    }

    public Vector(Point p){
        this.x = p.x;
        this.y = p.y;
    }

    public Vector(Vector p){
    	this.x = p.x;
        this.y = p.y;
    }

    public Point getPoint() {
        return new Point(x, y);
    }
    
    public static Point[] getPoints(Vector[] vectors) {
    	int len = vectors.length;
    	
    	Point[] points = new Point[len];
    	for (int i = 0; i < len; ++i) {
    		points[i] = new Point(vectors[i].x, vectors[i].y);
    	}
    	
    	return points;
    }

    public Vector multiply(double d) {
        return new Vector(x * d, y * d);
    }

    public Vector add(Vector b) {
        return add(b.getPoint());
    }
    
    public Vector add(Point b) {
        return new Vector(x+b.x, y+b.y);
    }
    
    public Vector add(double x, double y) {
    	return new Vector(this.x + x, this.y + y);
    }
    
    public Vector getUnitVector() {
    	return getLengthLimitedVector(1);
    }

    public double norm() {
        return Math.hypot(x, y);
    }
    
    public Vector getLengthLimitedVector(double l) {
    	double x = this.x,
    			y = this.y;
    	double norm = Math.hypot(x, y);
    	if(norm <= l) {
    		return new Vector(x,y);
    	}
    	x /= norm/l;
    	y /= norm/l;
		
    	return new Vector(x, y);
    }
    
    // public Vector unitVector() {
    //     final double t = norm();
    //     if (t < EPSILON){
    //         return this;
    //     } else {
    //         return multiply(1.0/t);
    //     }
    // }

    // public Vector getTaurusDistance(Vector from, double size) {
    //     return getTaurusDistance(from.getPoint(), size);
    // }

    // public Vector getTaurusDistance(Point from, double size) {
    //     double dx = x - from.x;
    //     double dy = y - from.y;
    //     if (Math.abs(dx) > Math.abs(x + size - from.x)) dx = x + size - from.x;
    //     if (Math.abs(dx) > Math.abs(x - size - from.x)) dx = x + size - from.x;
    //     if (Math.abs(dy) > Math.abs(y + size - from.x)) dy = y + size - from.y;
    //     if (Math.abs(dy) > Math.abs(y - size - from.x)) dy = y - size - from.y;
    //     return new Vector(dx, dy);
    // }

    public Vector rotate(double angle) {
        double newx, newy;
        newx = x*Math.cos(angle) - y*Math.sin(angle);
        newy = y*Math.cos(angle) + x*Math.sin(angle);
        return new Vector(newx, newy);
    }

    public double crossProduct(Vector v) {
        return (x * v.y) - (y * v.x);
    }

    // public getSideOfLine(Vector line, )

    public String toString() {
        return "("+String.format("%.6f", x)+","+String.format("%.6f", y)+")";
    }
}
