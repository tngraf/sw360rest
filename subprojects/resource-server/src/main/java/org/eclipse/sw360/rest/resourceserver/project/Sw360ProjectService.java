/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.project;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ProjectService {
    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;

    @NonNull
    private final Sw360UserService sw360UserService;

    public List<Project> getProjectsForUser(String userId) {
        try {
            ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
            User sw360User = sw360UserService.getUserByEmail(userId);
            return sw360ProjectClient.getAccessibleProjectsSummary(sw360User);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Project getProjectForUserById(String projectId, String userId) {
        try {
            ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
            User sw360User = sw360UserService.getUserByEmail(userId);
            return sw360ProjectClient.getProjectById(projectId, sw360User);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Project createProject(Project project, String userId) {
        try {
            ProjectService.Iface sw360ProjectClient = getThriftProjectClient();
            User sw360User = sw360UserService.getUserByEmail(userId);
            AddDocumentRequestSummary documentRequestSummary = sw360ProjectClient.addProject(project, sw360User);
            if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.SUCCESS) {
                project.setId(documentRequestSummary.getId());
                return project;
            } else if (documentRequestSummary.getRequestStatus() == AddDocumentRequestStatus.DUPLICATE) {
                throw new DataIntegrityViolationException("sw360 project with name '" + project.getName() + " already exists.");
            }
        } catch (TException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private ProjectService.Iface getThriftProjectClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/projects/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ProjectService.Client(protocol);
    }
}
