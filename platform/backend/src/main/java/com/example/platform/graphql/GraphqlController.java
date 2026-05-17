package com.example.platform.graphql;

import com.example.platform.common.web.RequestContexts;
import com.example.platform.documents.application.DocumentsFacade;
import com.example.platform.tasks.application.TasksFacade;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class GraphqlController {

    private final TasksFacade tasksFacade;
    private final DocumentsFacade documentsFacade;

    public GraphqlController(TasksFacade tasksFacade, DocumentsFacade documentsFacade) {
        this.tasksFacade = tasksFacade;
        this.documentsFacade = documentsFacade;
    }

    @QueryMapping
    public TasksFacade.TaskView task(@Argument String id) {
        return tasksFacade.getTask(id, RequestContexts.current().userId());
    }

    @QueryMapping
    public List<TasksFacade.TaskView> tasks(@Argument String workspaceId) {
        return tasksFacade.listTasks(workspaceId, RequestContexts.current().userId(), 0, 50);
    }

    @QueryMapping
    public DocumentsFacade.DocumentView document(@Argument String id) {
        return documentsFacade.getDocument(id, RequestContexts.current().userId());
    }

    @QueryMapping
    public List<DocumentsFacade.DocumentView> documents(@Argument String workspaceId) {
        return documentsFacade.listDocuments(workspaceId, RequestContexts.current().userId(), 0, 50);
    }

    @QueryMapping
    public DashboardView dashboard(@Argument String workspaceId) {
        String userId = RequestContexts.current().userId();
        List<TasksFacade.TaskView> tasks = tasksFacade.listTasks(workspaceId, userId, 0, 50);
        List<DocumentsFacade.DocumentView> documents = documentsFacade.listDocuments(workspaceId, userId, 0, 50);
        return new DashboardView(tasks, documents);
    }

    public static record DashboardView(List<TasksFacade.TaskView> tasks, List<DocumentsFacade.DocumentView> documents) {
    }
}

