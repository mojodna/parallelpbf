package akashihi.osm.parallelpbf.entity;

import lombok.Data;

/**
 * Single relation participant.
 *
 * @see Relation
 */
@Data
public final class RelationMember {
    /**
     * Defines relation member types.
     *
     * The values of the enum participants are linked to
     * the underlying protobuf definitions.
     */
    public enum Type {
        /**
         * Relation member is Node.
         *
         * @see Node
         */
        NODE(0),

        /**
         * Relation member is Way.
         *
         * @see Way
         */
        WAY(1),

        /**
         * Relation member is another Relation.
         *
         * @see Relation
         */
        RELATION(2);

        /**
         * A related protobuf relation member id.
         */
        private final int value;

        /**
         * Constructor for enum entry value.
         * @param v Protobuf relation member id.
         *
         * @see crosby.binary.Osmformat.Relation.MemberType
         */
        Type(final int v) {
            this.value = v;
        }

        /**
         * Finds proper enum entry by protobuf MemberType value.
         * @param v Protobuf relation member id.
         * @return Matching enum entry.
         * @throws IllegalArgumentException in case of unknown member id.
         */
        public static Type get(final int v) {
            for (Type t : Type.values()) {
                if (t.value == v) {
                    return t;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    /**
     * Id of referenced entity.
     */
    private final Long id;

    /**
     * Role of the referenced entity in the relation.
     * Can be null.
     */
    private final String role;

    /**
     * Type of the referencing entity.
     *
     * @see RelationMember.Type
     */
    private final Type type;
}
