/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2013 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.features.topology.app.internal.ui;

import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import org.opennms.features.topology.api.GraphContainer;
import org.opennms.features.topology.api.support.VertexHopGraphProvider;
import org.opennms.features.topology.api.topo.VertexRef;

public class NoContentAvailableWindow extends Window {

    private final Label noDefaultsAvailable;

    public NoContentAvailableWindow(final GraphContainer graphContainer) {
        super("No focus defined");

        setResizable(false);
        setClosable(false);
        setDraggable(true);
        setModal(false);
        setWidth(500, Sizeable.Unit.PIXELS);
        setHeight(300, Sizeable.Unit.PIXELS);

        Label label = new Label("This means" +
                "<ul>" +
                "<li>the last vertex was removed from focus or</li>" +
                "<li>no default focus is available.</li>" +
                "</ul>" +
                "To add a node to focus" +
                "<ul>" +
                "<li>manually add a node to focus via the search box</li>" +
                "<li>use the default focus</li>" +
                "</ul>",  ContentMode.HTML);

        final HorizontalLayout defaultLayout = new HorizontalLayout();
        defaultLayout.setMargin(true);
        defaultLayout.setSpacing(true);
        noDefaultsAvailable = new Label("No nodes found.<br/>Please add nodes manually.", ContentMode.HTML);
        noDefaultsAvailable.setVisible(false);

        Button defaultFocusButton = new Button("Use Default Focus");
        defaultFocusButton.addClickListener(new Button.ClickListener() {

            @Override
            public void buttonClick(Button.ClickEvent event) {
                VertexRef defaultRef = graphContainer.getBaseTopology().getDefaultFocus();
                if (defaultRef != null) {
                    VertexHopGraphProvider.FocusNodeHopCriteria criteria = VertexHopGraphProvider.getFocusNodeHopCriteriaForContainer(graphContainer, true);
                    criteria.add(defaultRef);
                    graphContainer.redoLayout();
                    noDefaultsAvailable.setVisible(false);
                } else {
                    noDefaultsAvailable.setVisible(true);
                }
            }
        });
        defaultLayout.setMargin(true);
        defaultLayout.addComponent(defaultFocusButton);
        defaultLayout.addComponent(noDefaultsAvailable);

        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setMargin(true);
        contentLayout.addComponent(label);
        contentLayout.addComponent(defaultLayout);

        setContent(contentLayout);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible) {
            noDefaultsAvailable.setVisible(false);
        } else {
            center();
        }
    }
}
