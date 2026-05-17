package com.example.platform.grpc;

import com.example.platform.documents.application.DocumentsFacade;
import com.example.platform.tasks.application.TasksFacade;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import io.grpc.BindableService;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlatformQueryGrpcService implements BindableService {

    public static final String SERVICE_NAME = "platform.v1.PlatformQueryService";

    static final MethodDescriptor<StringValue, Struct> LIST_TASKS_METHOD =
            MethodDescriptor.<StringValue, Struct>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "ListTasks"))
                    .setRequestMarshaller(ProtoUtils.marshaller(StringValue.getDefaultInstance()))
                    .setResponseMarshaller(ProtoUtils.marshaller(Struct.getDefaultInstance()))
                    .build();

    static final MethodDescriptor<StringValue, Struct> LIST_DOCUMENTS_METHOD =
            MethodDescriptor.<StringValue, Struct>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "ListDocuments"))
                    .setRequestMarshaller(ProtoUtils.marshaller(StringValue.getDefaultInstance()))
                    .setResponseMarshaller(ProtoUtils.marshaller(Struct.getDefaultInstance()))
                    .build();

    private final TasksFacade tasksFacade;
    private final DocumentsFacade documentsFacade;

    public PlatformQueryGrpcService(TasksFacade tasksFacade, DocumentsFacade documentsFacade) {
        this.tasksFacade = tasksFacade;
        this.documentsFacade = documentsFacade;
    }

    @Override
    public ServerServiceDefinition bindService() {
        return ServerServiceDefinition.builder(SERVICE_NAME)
                .addMethod(LIST_TASKS_METHOD, ServerCalls.asyncUnaryCall(this::listTasks))
                .addMethod(LIST_DOCUMENTS_METHOD, ServerCalls.asyncUnaryCall(this::listDocuments))
                .build();
    }

    private void listTasks(StringValue request, StreamObserver<Struct> responseObserver) {
        try {
            List<TasksFacade.TaskView> tasks = tasksFacade.listTasks(request.getValue());
            responseObserver.onNext(taskResponse(tasks));
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    private void listDocuments(StringValue request, StreamObserver<Struct> responseObserver) {
        try {
            List<DocumentsFacade.DocumentView> documents = documentsFacade.listDocuments(request.getValue());
            responseObserver.onNext(documentResponse(documents));
            responseObserver.onCompleted();
        } catch (RuntimeException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(exception.getMessage())
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    private Struct taskResponse(List<TasksFacade.TaskView> tasks) {
        ListValue.Builder values = ListValue.newBuilder();
        tasks.stream().map(this::taskValue).forEach(values::addValues);
        return Struct.newBuilder()
                .putFields("tasks", Value.newBuilder().setListValue(values).build())
                .build();
    }

    private Struct documentResponse(List<DocumentsFacade.DocumentView> documents) {
        ListValue.Builder values = ListValue.newBuilder();
        documents.stream().map(this::documentValue).forEach(values::addValues);
        return Struct.newBuilder()
                .putFields("documents", Value.newBuilder().setListValue(values).build())
                .build();
    }

    private Value taskValue(TasksFacade.TaskView task) {
        Struct taskStruct = Struct.newBuilder()
                .putFields("taskId", stringValue(task.taskId()))
                .putFields("workspaceId", stringValue(task.workspaceId()))
                .putFields("title", stringValue(task.title()))
                .putFields("description", stringValue(task.description()))
                .putFields("status", stringValue(task.status()))
                .putFields("assigneeUserId", nullableStringValue(task.assigneeUserId()))
                .putFields("createdByUserId", stringValue(task.createdByUserId()))
                .build();
        return Value.newBuilder().setStructValue(taskStruct).build();
    }

    private Value documentValue(DocumentsFacade.DocumentView document) {
        Struct documentStruct = Struct.newBuilder()
                .putFields("documentId", stringValue(document.documentId()))
                .putFields("workspaceId", stringValue(document.workspaceId()))
                .putFields("title", stringValue(document.title()))
                .putFields("content", stringValue(document.content()))
                .putFields("status", stringValue(document.status()))
                .putFields("createdByUserId", stringValue(document.createdByUserId()))
                .putFields("lastModifiedByUserId", stringValue(document.lastModifiedByUserId()))
                .build();
        return Value.newBuilder().setStructValue(documentStruct).build();
    }

    private Value stringValue(String value) {
        return Value.newBuilder().setStringValue(value).build();
    }

    private Value nullableStringValue(String value) {
        if (value == null) {
            return Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
        }
        return stringValue(value);
    }
}
