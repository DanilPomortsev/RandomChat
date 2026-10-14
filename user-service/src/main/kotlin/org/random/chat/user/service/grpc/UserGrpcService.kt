package org.random.chat.user.service.grpc

import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.random.chat.user.api.GetUserRequest
import org.random.chat.user.api.SaveUserRequest
import org.random.chat.user.api.SaveUserResponse
import org.random.chat.user.api.User
import org.random.chat.user.api.UserServiceGrpc
import org.random.chat.user.service.service.UserService
import org.springframework.stereotype.Service

@Service
class UserGrpcService(
    private val userService: UserService,
) : UserServiceGrpc.UserServiceImplBase() {
    override fun getUser(request: GetUserRequest, responseObserver: StreamObserver<User>) {
        val user = userService.getById(request.id)
        if (user != null) {
            responseObserver.onNext(user.toProto())
            responseObserver.onCompleted()
        } else {
            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription("User ${request.id} was not found")
                    .asRuntimeException(),
            )
        }
    }

    override fun saveUser(request: SaveUserRequest, responseObserver: StreamObserver<SaveUserResponse>) {
        val userId = userService.save(
            email = request.email,
            nickname = request.nickname,
            avatarUrl = request.avatarUrl,
        )
        responseObserver.onNext(SaveUserResponse.newBuilder().setId(userId).build())
        responseObserver.onCompleted()
    }
}
