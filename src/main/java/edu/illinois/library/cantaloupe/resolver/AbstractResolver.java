package edu.illinois.library.cantaloupe.resolver;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.processor.StreamProcessor;

abstract class AbstractResolver {

    protected Identifier identifier;
    protected Format sourceFormat;

    public final boolean isCompatible(Processor processor) {
         return !(!(this instanceof FileResolver) &&
                !(processor instanceof StreamProcessor));
    }

    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
        this.sourceFormat = null;
    }

}