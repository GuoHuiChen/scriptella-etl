/*
 * Copyright 2006 The Scriptella Project Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scriptella.configuration;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import scriptella.spi.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;


/**
 * TODO: Add documentation
 *
 * @author Fyodor Kupolov
 * @version 1.0
 */
public class ContentEl extends XmlConfigurableBase implements Resource {
    private List<Resource> content = new ArrayList<Resource>();
    /**
     * Null-Object to use instead of null if necessary
     */
    public static final Resource NULL_CONTENT = new StringResource("", "Empty Content");

    public ContentEl() {
    }

    public ContentEl(XmlElement element) {
        configure(element);
    }

    public Reader open() throws IOException {
        return new BufferedReader(new MultipartReader());
    }

    public void configure(final XmlElement element) {
        for (Node node = element.getElement().getFirstChild(); node != null; node = node.getNextSibling()) {
            Resource resource = asResource(element, node);
            if (resource != null) {
                content.add(resource);
            }
        }
    }


    /**
     * Creates a resource using content from the specified node.
     * <p>If node is not textual content or not include element, the content is skipped and null is returned.
     *
     * @param parentElement parent element of this node.
     * @param node          node to get content from.
     * @return parsed resource or null.
     */
    static Resource asResource(final XmlElement parentElement, final Node node) {
        if (node == null) {
            return null;
        }
        if (node instanceof Text) {
            return new StringResource(((Text) node).getData());
        } else if (node instanceof Element && "include".equals(node.getNodeName())) {
            return new IncludeEl(new XmlElement((Element) node, parentElement));
        }
        return null;


    }

    /**
     * Merges this content with specified one and returns this element.
     *
     * @param contentEl content to merge with.
     * @return this instance with merged content.
     */
    final ContentEl merge(final ContentEl contentEl) {
        content.addAll(contentEl.content);
        return this;
    }

    /**
     * Appends a resource to this content.
     *
     * @param resource resource to append.
     */
    final void append(final Resource resource) {
        content.add(resource);
    }

    public String toString() {
        Location loc = getLocation();
        return loc != null ? loc.toString() : ("ContentEl{" + "content=" + content + '}');
    }

    class MultipartReader extends Reader {
        private int pos = 0;
        private Reader current;

        public int read(final char cbuf[], final int off, final int len) throws IOException {
            if ((pos < 0) || ((pos >= content.size()) && (current == null))) {
                return -1;
            }

            int l = len;
            int o = off;

            while (l > 0) {
                if (current == null) {
                    if (pos < content.size()) {
                        current = content.get(pos).open();
                        pos++;
                    } else {
                        return ((len - l) <= 0) ? (-1) : (len - l);
                    }
                } else {
                    final int r = current.read(cbuf, o, l);

                    if (r <= 0) {
                        current.close();
                        current = null;
                    } else if (r <= l) {
                        l -= r;
                        o += r;
                    }
                }
            }

            return len - l;
        }

        public void close() throws IOException {
            if (current != null) {
                current.close();
                current = null;
            }

            pos = -1;
        }
    }
}
