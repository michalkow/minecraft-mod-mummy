package me.kowalkowski.zombiekingdom.client.render;

/**
 * One piece of hanging cloth (a bandage strip, a cape): a chain of segments parented to a part of
 * the zombie skeleton. See {@link ClothModel} for how it moves.
 *
 * @param parent skeleton part it hangs from: head, body, right_arm or left_arm
 * @param x attachment point, in the parent's space
 * @param width strip width (depth for sideways strips)
 * @param thickness 0 for a flat plane, or a real thickness (capes)
 * @param sideways lies in the YZ plane (seen from the side) instead of the XY plane; flat strips only
 * @param side which way the strip must not swing, so it never clips into the part it hangs from
 * @param drag how strongly walking and falling push the strip backwards
 * @param flutter scales the wave; heavy cloth flutters less
 * @param phase offsets the wave so strips don't move in lockstep
 * @param yaw turns the strip diagonally so its face shows from most angles
 * @param flare how far the strip spreads outwards from the part it hangs from (skirt panels)
 * @param clearsLegs swings the strip out of the way of the legs (skirt panels)
 */
public record ClothStrip(
	String name, String parent, float x, float y, float z, int width, int segmentLength, int segments, int thickness, int u, int v,
	boolean sideways, Side side, float drag, float flutter, float phase, float yaw, float flare, boolean clearsLegs
) {
	public enum Side {
		FREE, BACK, FRONT
	}

	/** Rows of texture each segment occupies (box UV layout: depth rows, then the faces). */
	public int textureRowsPerSegment() {
		return (this.sideways ? this.width : this.thickness) + this.segmentLength;
	}

	/** A thin flat strip, such as a loose bandage. */
	public static ClothStrip ribbon(
		final String name, final String parent, final float x, final float y, final float z, final int width, final int segmentLength, final int segments,
		final int u, final int v, final Side side, final float drag, final float phase, final float yaw
	) {
		return new ClothStrip(name, parent, x, y, z, width, segmentLength, segments, 0, u, v, false, side, drag, 1.0F, phase, yaw, 0.0F, false);
	}

	/** A thin flat strip that faces sideways. */
	public static ClothStrip sidewaysRibbon(
		final String name, final String parent, final float x, final float y, final float z, final int width, final int segmentLength, final int segments,
		final int u, final int v, final float drag, final float phase, final float yaw
	) {
		return new ClothStrip(name, parent, x, y, z, width, segmentLength, segments, 0, u, v, true, Side.FREE, drag, 1.0F, phase, yaw, 0.0F, false);
	}

	/** A one-pixel-thick cape hanging behind the body. */
	public static ClothStrip cape(
		final String name, final float x, final float y, final float z, final int width, final int segmentLength, final int segments, final int u, final int v
	) {
		return new ClothStrip(name, "body", x, y, z, width, segmentLength, segments, 1, u, v, false, Side.BACK, 1.0F, 0.7F, 0.0F, 0.0F, 0.0F, false);
	}

	/**
	 * One flared panel of a skirt hanging from the waist: front and back panels face forwards,
	 * side panels ({@code side == FREE}) face sideways. Panels swing aside for the legs.
	 */
	public static ClothStrip skirt(
		final String name, final float x, final float y, final float z, final int width, final int segmentLength, final int segments,
		final int u, final int v, final Side side, final float phase
	) {
		return new ClothStrip(name, "body", x, y, z, width, segmentLength, segments, 0, u, v, side == Side.FREE, side, 0.3F, 0.6F, phase, 0.0F, 0.3F, true);
	}
}
