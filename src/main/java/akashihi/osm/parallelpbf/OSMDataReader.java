package akashihi.osm.parallelpbf;

import akashihi.osm.parallelpbf.entity.Node;
import akashihi.osm.parallelpbf.entity.Relation;
import akashihi.osm.parallelpbf.entity.Way;
import akashihi.osm.parallelpbf.parser.NodeParser;
import akashihi.osm.parallelpbf.parser.RelationParser;
import akashihi.osm.parallelpbf.parser.WayParser;
import com.google.protobuf.InvalidProtocolBufferException;
import crosby.binary.Osmformat;
import lombok.extern.slf4j.Slf4j;
import lombok.var;

import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * Implemented parser for OSMHeader message.
 *
 * @see akashihi.osm.parallelpbf.parser.BaseParser
 */
@Slf4j
public class OSMDataReader extends OSMReader {
    /**
     * Changeset processing callback. Must be reentrant.
     */
    private final Consumer<Long> changesetsCb;

    /**
     * Nodes processing callback. Must be reentrant.
     */
    private final Consumer<Node> nodesCb;

    /**
     * Ways processing callback. Must be reentrant.
     */
    private final Consumer<Way> waysCb;

    /**
     * Relations processing callback. Must be reentrant.
     */
    private final Consumer<Relation> relationsCb;

    /**
     * Configures reader with blob and callbacks.
     * @param blob         blob to parse.
     * @param tasksLimiter task limiting semaphore.
     * @param onNodes Callback to call on node parse. May be null, in that case nodes parsing will be skipped.
     * @param onWays Callback to call on way parse. May be null, in that case ways parsing will be skipped.
     * @param onRelations Callback to call on relation parse. May be null,
     *                    in that case relations parsing will be skipped.
     * @param onChangesets Callback to call on changeset parse. May be null,
     *                     in that case changesets parsing will be skipped.
     */
    OSMDataReader(final byte[] blob,
                  final Semaphore tasksLimiter,
                  final Consumer<Node> onNodes,
                  final Consumer<Way> onWays,
                  final Consumer<Relation> onRelations,
                  final Consumer<Long> onChangesets) {
        super(blob, tasksLimiter);
        this.nodesCb = onNodes;
        this.waysCb = onWays;
        this.relationsCb = onRelations;
        this.changesetsCb = onChangesets;
    }

    /**
     * Extracts primitives groups from the Blob and parses them.
     * <p>
     * In case callback for some of the primitives is not set, it will
     * be ignored and not parsed.
     *
     * @param message Raw OSMData blob.
     * @throws RuntimeException in case of protobuf parsing error.
     */
    @Override
    protected void read(final byte[] message) {
        Osmformat.PrimitiveBlock primitives;
        try {
            primitives = Osmformat.PrimitiveBlock.parseFrom(message);
        } catch (InvalidProtocolBufferException e) {
            log.error("Error parsing OSMData block: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        var stringTable = primitives.getStringtable();
        var groups = primitives.getPrimitivegroupList();
        for (Osmformat.PrimitiveGroup group : groups) {
            if (nodesCb != null) {
                var parser = new NodeParser<Osmformat.Node, Consumer<Node>>(nodesCb,
                        stringTable,
                        primitives.getGranularity(),
                        primitives.getLatOffset(),
                        primitives.getLonOffset(),
                        primitives.getDateGranularity());
                group.getNodesList().forEach(parser::parse);
                if (group.hasDense()) {
                    parser.parse(group.getDense());
                }
            }
            if (waysCb != null) {
                var parser = new WayParser<Osmformat.Way, Consumer<Way>>(waysCb, stringTable);
                group.getWaysList().forEach(parser::parse);
            }
            if (relationsCb != null) {
                var parser = new RelationParser<Osmformat.Relation, Consumer<Relation>>(relationsCb, stringTable);
                group.getRelationsList().forEach(parser::parse);
            }
            if (changesetsCb != null) {
                group.getChangesetsList().forEach(changeMessage -> {
                    long id = changeMessage.getId();
                    log.debug("ChangeSet id: {}", id);
                    changesetsCb.accept(id);
                });
            }
        }
    }
}