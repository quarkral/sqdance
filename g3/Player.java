package sqdance.g3;

import sqdance.sim.Point;

public class Player implements sqdance.sim.Player {
    private sqdance.sim.Player player;

    // THESE NEED TO BE DETERMINED
    private final int MEDIUM_SIZE = 0;
    private final int LARGE_SIZE = 1841;

    @Override
    public void init(int d, int room_side) {

        if (d < MEDIUM_SIZE) {
            // Soulmate-finding strategy
            System.out.print("Using small player.");
            this.player = new SmallPlayer();
        } else if (d < LARGE_SIZE) {
            // Pair off dancers with strangers
            System.out.print("Using medium player.");
            this.player = new MediumPlayer();
        } else {
            // Keep some dancers off the dance floor
            System.out.print("Using large player.");
            this.player = new LargePlayer();
        }

        this.player.init(d, room_side);
    }

    @Override
    public Point[] generate_starting_locations() {
        return this.player.generate_starting_locations();
    }

    @Override
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        return this.player.play(dancers, scores, partner_ids, enjoyment_gained);

    }
}
