package com.wolt.osm.parallelpbf.encoder;

import com.wolt.osm.parallelpbf.entity.Node;
import crosby.binary.Osmformat;

/**
 * Encodes for DenseNodes structure. Keeps data for the next blob
 * production in RAM and form byte[] blob in request.
 *
 * Encoder is stateful and can't be used after 'write' call is issued.
 * Encoder is not thread-safe.
 */
public final class DenseNodesEncoder extends OsmEntityEncoder {
    /**
     * Coordinates grid default granularity.
     */
    private static final int GRANULARITY = 100;

    /**
     * Single mode uses 3 long values: id, lat, lon.
     * So single node will use 24 bytes.
     */
    private static final int NODE_ENTRY_SIZE = 24;

    /**
     * Current value of NodeId for delta coding.
     */
    private long id = 0;

    /**
     * Current value of lat millis for delta coding.
     */
    private long lat = 0;

    /**
     * Current value of lon millis for delta coding.
     */
    private long lon = 0;

    /**
     * DensNodes blob.
     */
    private Osmformat.DenseNodes.Builder nodes = Osmformat.DenseNodes.newBuilder();

    /**
     * 'Write was called' flag.
     */
    private boolean built = false;

    /**
     * Default constructor.
     */
    public DenseNodesEncoder() {
        super();
    }

    /**
     * Adds a node to the encoder.
     * @param node Node to add.
     * @throws IllegalStateException when call after write() call.
     */
    public void addNode(final Node node) {
        if (built) {
            throw new IllegalStateException("Encoder content is already written");
        }
        node.getTags().forEach((k, v) -> {
            nodes.addKeysVals(getStringIndex(k));
            nodes.addKeysVals(getStringIndex(v));
        });
        nodes.addKeysVals(0); //Index zero means 'end of tags for node'

        nodes.addId(node.getId() - id);
        id = node.getId();

        long latMillis = doubleToNanoScaled(node.getLat() / GRANULARITY);
        long lonMillis = doubleToNanoScaled(node.getLon() / GRANULARITY);

        nodes.addLat(latMillis - lat);
        nodes.addLon(lonMillis - lon);
        lat = latMillis;
        lon = lonMillis;
    }

    /**
     * Provides approximate size of the future blob.
     * Size is calculated as length of all strings in the string tables
     * plus 24 bytes per each node plus 4 bytes per each tag, including closing tags.
     * As protobuf will compact the values in arrays, actual size expected to be smaller.
     * @return Estimated approximate maximum size of a blob.
     */
    @Override
    public int estimateSize() {
        return this.getStringSize() + nodes.getIdCount() * NODE_ENTRY_SIZE + nodes.getKeysValsCount() * TAG_ENTRY_SIZE;
    }

    @Override
    public byte[] write() {
        built = true;
        Osmformat.PrimitiveGroup.Builder nodesGroup = Osmformat.PrimitiveGroup.newBuilder().setDense(nodes);
        return Osmformat.PrimitiveBlock.newBuilder()
                .setGranularity(GRANULARITY)
                .setLatOffset(0)
                .setLonOffset(0)
                .setStringtable(this.getStrings())
                .addPrimitivegroup(nodesGroup)
                .build()
                .toByteArray();
    }
}
