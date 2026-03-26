/*
 * Dataverse Downloader
 *
 * Copyright (c) 2026 Service Center for Research Data Management,
 * University of Bonn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author: Sergej Zerr
 * Organization: Service Center for Research Data Management, University of Bonn
 */
package de.unibn.hrz.dataverse.downloader.ui;

import javax.swing.tree.DefaultMutableTreeNode;

public class CheckboxTreeNode extends DefaultMutableTreeNode {
    private static final long serialVersionUID = -764484377345984419L;

    public enum DownloadState {
        NONE,
        PARTIAL,
        COMPLETED
    }

    private boolean selected;
    private DownloadState downloadState = DownloadState.NONE;

    public CheckboxTreeNode(Object userObject) {
        super(userObject);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        setSelected(selected, true);
    }

    public void setSelected(boolean selected, boolean propagateToChildren) {
        this.selected = selected;

        if (!propagateToChildren) {
            return;
        }

        for (int i = 0; i < getChildCount(); i++) {
            Object child = getChildAt(i);
            if (child instanceof CheckboxTreeNode checkboxTreeNode) {
                checkboxTreeNode.setSelected(selected, true);
            }
        }
    }

    public DownloadState getDownloadState() {
        return downloadState;
    }

    public void setDownloadState(DownloadState downloadState) {
        this.downloadState = downloadState == null ? DownloadState.NONE : downloadState;
    }

    public boolean isCompleted() {
        return downloadState == DownloadState.COMPLETED;
    }

    public boolean isPartial() {
        return downloadState == DownloadState.PARTIAL;
    }
}