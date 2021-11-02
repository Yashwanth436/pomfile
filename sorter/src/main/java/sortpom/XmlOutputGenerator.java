package sortpom;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.DefaultText;
import org.dom4j.tree.NamespaceStack;
import sortpom.exception.FailureException;
import sortpom.content.NewlineText;
import sortpom.parameter.PluginParameters;
import sortpom.util.StringLineSeparatorWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;

/**
 * Handles all generation of xml.
 */
public class XmlOutputGenerator {
    private String encoding;
    private String indentCharacters;
    private boolean expandEmptyElements;
    private boolean indentBlankLines;
    private boolean indentSchemaLocation;
    private boolean spaceBeforeCloseEmptyElement;
    private String lineSeparator;

    /**
     * Setup default configuration
     */
    public void setup(PluginParameters pluginParameters) {
        this.indentCharacters = pluginParameters.indentCharacters;
        this.encoding = pluginParameters.encoding;
        this.expandEmptyElements = pluginParameters.expandEmptyElements;
        this.indentBlankLines = pluginParameters.indentBlankLines;
        this.indentSchemaLocation = pluginParameters.indentSchemaLocation;
        this.spaceBeforeCloseEmptyElement = pluginParameters.spaceBeforeCloseEmptyElement;
        lineSeparator = pluginParameters.lineSeparatorUtil.toString();
    }

    /**
     * Returns the sorted xml as an OutputStream.
     *
     * @return the sorted xml
     */
    public String getSortedXml(Document newDocument) {
        try (StringWriter out = new StringWriter();
             StringLineSeparatorWriter writer = new StringLineSeparatorWriter(out, lineSeparator)) {
            XMLWriter xmlWriter = new PatchedXMLWriter(writer, createPrettyFormat(), spaceBeforeCloseEmptyElement,
                indentBlankLines, indentSchemaLocation);
            xmlWriter.write(newDocument);
            writer.writeDelayedNewline();
            return writer.toString();
        } catch (IOException ioex) {
            throw new FailureException("Could not format pom files content", ioex);
        }
    }

    private OutputFormat createPrettyFormat() {
        OutputFormat outputFormat = new OutputFormat(indentCharacters);
        outputFormat.setNewlines(true);
        outputFormat.setExpandEmptyElements(expandEmptyElements);
        outputFormat.setNewLineAfterDeclaration(false);
        outputFormat.setEncoding(encoding);
        return outputFormat;
    }

    private static class PatchedXMLWriter extends XMLWriter {
        private final OutputFormat format;
        private final boolean indentBlankLines;
        private final boolean indentSchemaLocation;
        private final boolean spaceBeforeCloseEmptyElement;
        private final NamespaceStack parentNamespaceStack;

        public PatchedXMLWriter(Writer writer, OutputFormat format, boolean spaceBeforeCloseEmptyElement, boolean indentBlankLines, boolean indentSchemaLocation) {
            super(writer, format);
            this.format = format;
            this.indentBlankLines = indentBlankLines;
            this.indentSchemaLocation = indentSchemaLocation;
            this.spaceBeforeCloseEmptyElement = spaceBeforeCloseEmptyElement;
            this.parentNamespaceStack = findParentNamespaceStack();
        }

        private NamespaceStack findParentNamespaceStack() {
            try {
                Field field = XMLWriter.class.getDeclaredField("namespaceStack");
                field.setAccessible(true);
                return (NamespaceStack) field.get(this);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new FailureException("Cannot access internal namespace stack in XMLWriter", e);
            }
        }

        @Override
        protected void writeEmptyElementClose(String qualifiedName)
            throws IOException {
            if (!format.isExpandEmptyElements()) {
                if (spaceBeforeCloseEmptyElement) {
                    // add an extra place before closing tag 
                    writer.write(' ');
                }
                writer.write("/>");
            } else {
                writer.write("></");
                writer.write(qualifiedName);
                writer.write(">");
            }
        }

        @Override
        protected void writeProcessingInstruction(ProcessingInstruction pi)
            throws IOException {
            // Place the processing instruction on own line (instead of same line as previous element)
            writePrintln();
            indent();
            writer.write("<?");
            writer.write(pi.getName());
            writer.write(" ");
            writer.write(pi.getText());
            writer.write("?>");

            lastOutputNodeType = Node.PROCESSING_INSTRUCTION_NODE;
        }

        @Override
        protected void writeDocType(DocumentType docType) throws IOException {
            // Place the doc type on own line (instead of same line as previous element)
            if (docType != null) {
                writePrintln();
                indent();
                docType.write(writer);
            }
        }

        @Override
        protected void writeNodeText(Node node) throws IOException {
            if (node instanceof NewlineText) {
                // Handle our own NewlineText
                writePrintln();
                if (indentBlankLines) {
                    // If blank lines should be indented
                    indent();
                }
            } else {
                // Check if attribute xml:preserve is used
                if (isElementSpacePreserved(node.getParent())) {
                    super.writeNodeText(node);
                } else {
                    // Otherwise trim the text content
                    String text = node.getText();
                    super.write(new DefaultText(text == null ? null : text.trim()));
                }
            }
        }

        @Override
        protected void writeAttributes(Element element) throws IOException {
            for (int i = 0, size = element.attributeCount(); i < size; i++) {
                Attribute attribute = element.attribute(i);
                Namespace ns = attribute.getNamespace();

                if ((ns != null) && (ns != Namespace.NO_NAMESPACE)
                    && (ns != Namespace.XML_NAMESPACE)) {
                    String prefix = ns.getPrefix();
                    String uri = parentNamespaceStack.getURI(prefix);

                    if (!ns.getURI().equals(uri)) {
                        writeNamespace(ns);
                        parentNamespaceStack.push(ns);
                    }
                }

                String attName = attribute.getName();

                if (attName.startsWith("xmlns:")) {
                    String prefix = attName.substring(6);

                    if (parentNamespaceStack.getNamespaceForPrefix(prefix) == null) {
                        String uri = attribute.getValue();
                        parentNamespaceStack.push(prefix, uri);
                        writeNamespace(prefix, uri);
                    }
                } else if (attName.equals("xmlns")) {
                    if (parentNamespaceStack.getDefaultNamespace() == null) {
                        String uri = attribute.getValue();
                        parentNamespaceStack.push(null, uri);
                        writeNamespace(null, uri);
                    }
                } else {
                    writeAttribute(attribute);
                }
            }
        }

        @Override
        protected void writeAttribute(Attribute attribute) throws IOException {
            String qualifiedName = attribute.getQualifiedName();
            if (indentSchemaLocation && "xsi:schemaLocation".equals(qualifiedName)) {
                writePrintln();
                writeString(format.getIndent());
                writeString(format.getIndent());
            }
            writer.write(" ");
            writer.write(qualifiedName);
            writer.write("=");

            char quote = format.getAttributeQuoteCharacter();
            writer.write(quote);

            writeEscapeAttributeEntities(attribute.getValue());

            writer.write(quote);
            lastOutputNodeType = Node.ATTRIBUTE_NODE;
        }

    }
}
