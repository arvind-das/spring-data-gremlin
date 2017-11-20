package org.springframework.data.gremlin.schema.writer.titan;

import com.thinkaurelius.titan.core.*;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.tinkerpop.blueprints.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.gremlin.schema.GremlinSchema;
import org.springframework.data.gremlin.schema.property.GremlinProperty;
import org.springframework.data.gremlin.schema.writer.AbstractSchemaWriter;
import org.springframework.data.gremlin.schema.writer.SchemaWriter;
import org.springframework.data.gremlin.schema.writer.SchemaWriterException;
import org.springframework.data.gremlin.tx.GremlinGraphFactory;
import org.springframework.data.gremlin.tx.titan.TitanGremlinGraphFactory;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.data.gremlin.schema.property.GremlinRelatedProperty.CARDINALITY;

/**
 * A concrete {@link SchemaWriter} for an OrientDB database.
 *
 * @author Gman
 */
public class TitanSchemaWriter extends AbstractSchemaWriter<VertexLabel, EdgeLabel, PropertyKey, TitanElement> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TitanSchemaWriter.class);

    private TitanManagement mgmt;

    public void initialise(GremlinGraphFactory tgf, GremlinSchema<?> schema) throws SchemaWriterException {

        try {
            TitanGraph graph = ((TitanGremlinGraphFactory) tgf).graph();
            mgmt = graph.getManagementSystem();

        } catch (RuntimeException e) {
            String msg = String.format("Could not create schema %s. ERROR: %s", schema, e.getMessage());
            throw new SchemaWriterException(msg, e);
        }
    }

    @Override
    @Transactional(readOnly = false)
    public void writeSchema(GremlinGraphFactory tgf, GremlinSchema<?> schema) throws SchemaWriterException {
        initialise(tgf, schema);
        super.writeSchema(tgf, schema);
    }

    @Override
    protected boolean isPropertyAvailable(Object vertexClass, String name) {
        Object prop = ((VertexLabel) vertexClass).getProperty(name);
        return prop != null;
    }

    @Override
    protected VertexLabel createVertexClass(GremlinSchema schema) throws Exception {
        VertexLabel vertexClass = mgmt.getVertexLabel(schema.getClassName());
        if (vertexClass == null) {
            vertexClass = mgmt.makeVertexLabel(schema.getClassName()).make();
            mgmt.commit();
        }
        return vertexClass;
    }

    @Override
    protected EdgeLabel createEdgeClass(GremlinSchema schema) throws Exception {
        EdgeLabel edgeClass = mgmt.getEdgeLabel(schema.getClassName());
        if (edgeClass == null) {
            edgeClass = mgmt.makeEdgeLabel(schema.getClassName()).make();
            mgmt.commit();
        }
        return edgeClass;
    }

    @Override
    protected void rollback(GremlinSchema schema) {

        try {
            mgmt.rollback();
        } catch (Exception e1) {
            LOGGER.error("Could not rollback: " + e1.getMessage(), e1);
        }

    }

    @Override
    protected EdgeLabel createEdgeClass(String name, VertexLabel outVertex, VertexLabel inVertex, CARDINALITY cardinality) throws SchemaWriterException {

        Multiplicity multiplicity = Multiplicity.SIMPLE;
        if (cardinality == CARDINALITY.ONE_TO_ONE) {
            multiplicity = Multiplicity.ONE2ONE;
        } else if (cardinality == CARDINALITY.ONE_TO_MANY) {
            multiplicity = Multiplicity.ONE2MANY;
        }

        return mgmt.makeEdgeLabel(name).directed().multiplicity(multiplicity).make();
    }

    @Override
    protected boolean isEdgeInProperty(EdgeLabel edgeClass) {
        return true;
    }

    @Override
    protected boolean isEdgeOutProperty(EdgeLabel edgeClass) {
        return true;
    }

    @Override
    protected PropertyKey setEdgeOut(EdgeLabel edgeClass, VertexLabel vertexClass) {
        return null;
    }

    @Override
    protected PropertyKey setEdgeIn(EdgeLabel edgeClass, VertexLabel vertexClass) {
        return null;
    }

    @Override
    protected PropertyKey createProperty(TitanElement parentElement, String name, Class<?> cls) {
        return mgmt.makePropertyKey(name).dataType(cls).make();
    }

    @Override
    protected void createNonUniqueIndex(PropertyKey property) {
        mgmt.buildIndex(property.getName(), Vertex.class).addKey(property).buildCompositeIndex();
    }

    @Override
    protected void createUniqueIndex(PropertyKey property) {
        mgmt.buildIndex(property.getName(), Vertex.class).addKey(property).unique().buildCompositeIndex();
    }

    @Override
    protected void createSpatialIndex(GremlinSchema<?> schema, GremlinProperty latitude, GremlinProperty longitude) {

    }

}
