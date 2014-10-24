/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.alerts.actions;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.alerts.Alert;
import org.elasticsearch.alerts.AlertManager;
import org.elasticsearch.alerts.AlertResult;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlertActionManager extends AbstractComponent {

    private final AlertManager alertManager;
    private volatile ImmutableOpenMap<String, AlertActionFactory> actionImplemented;

    @Inject
    public AlertActionManager(Settings settings, AlertManager alertManager, Client client) {
        super(settings);
        this.alertManager = alertManager;
        this.actionImplemented = ImmutableOpenMap.<String, AlertActionFactory>builder()
                .fPut("email", new EmailAlertActionFactory())
                .fPut("index", new IndexAlertActionFactory(client))
                .build();
        alertManager.setActionManager(this);
    }

    public void registerAction(String name, AlertActionFactory actionFactory){
        actionImplemented = ImmutableOpenMap.builder(actionImplemented)
                .fPut(name, actionFactory)
                .build();
    }

    public List<AlertAction> parseActionsFromMap(Map<String,Object> actionMap) {
        ImmutableOpenMap<String, AlertActionFactory> actionImplemented = this.actionImplemented;
        List<AlertAction> actions = new ArrayList<>();
        for (Map.Entry<String, Object> actionEntry : actionMap.entrySet()) {
            AlertActionFactory factory = actionImplemented.get(actionEntry.getKey());
            if (factory != null) {
                actions.add(factory.createAction(actionEntry.getValue()));
            } else {
                throw new ElasticsearchIllegalArgumentException("No action exists with the name [" + actionEntry.getKey() + "]");
            }
        }
        return actions;
    }

    public void doAction(String alertName, AlertResult alertResult){
        Alert alert = alertManager.getAlertForName(alertName);
        for (AlertAction action : alert.actions()) {
            action.doAction(alertName, alertResult);
        }
    }

}
