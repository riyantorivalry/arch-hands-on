package com.example.platform.graphql;

import com.example.platform.common.web.RequestContexts;
import com.example.platform.documents.application.DocumentsFacade;
import com.example.platform.tasks.application.TasksFacade;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class GraphqlController {

    private final TasksFacade tasksFacade;
    private final DocumentsFacade documentsFacade;

    public GraphqlController(TasksFacade tasksFacade, DocumentsFacade documentsFacade) {
        this.tasksFacade = tasksFacade;
        this.documentsFacade = documentsFacade;
    }

    @QueryMapping
    public Mono<TasksFacade.TaskView> task(@Argument String id) {
        return RequestContexts.currentReactive()
                .flatMap(context -> tasksFacade.getTask(id, context.userId()));
    }

    @QueryMapping
    public Mono<List<TasksFacade.TaskView>> tasks(@Argument String workspaceId) {
        return RequestContexts.currentReactive()
                .flatMap(context -> tasksFacade.listTasks(workspaceId, context.userId(), 0, 50));
    }

    @QueryMapping
    public Mono<DocumentsFacade.DocumentView> document(@Argument String id) {
        return RequestContexts.currentReactive()
                .flatMap(context -> documentsFacade.getDocument(id, context.userId()));
    }

    @QueryMapping
    public Mono<List<DocumentsFacade.DocumentView>> documents(@Argument String workspaceId) {
        return RequestContexts.currentReactive()
                .flatMap(context -> documentsFacade.listDocuments(workspaceId, context.userId(), 0, 50));
    }

    @QueryMapping
    public Mono<DashboardView> dashboard(@Argument String workspaceId) {
        return RequestContexts.currentReactive()
                .flatMap(context -> Mono.zip(
                        tasksFacade.listTasks(workspaceId, context.userId(), 0, 50),
                        documentsFacade.listDocuments(workspaceId, context.userId(), 0, 50),
                        DashboardView::new
                ));
    }

    public static record DashboardView(List<TasksFacade.TaskView> tasks, List<DocumentsFacade.DocumentView> documents) {
    }
}

