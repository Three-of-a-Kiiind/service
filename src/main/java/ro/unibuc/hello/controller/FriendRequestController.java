package ro.unibuc.hello.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.hello.data.FriendRequestEntity;
import ro.unibuc.hello.dto.User;
import ro.unibuc.hello.service.FriendRequestService;
import ro.unibuc.hello.metrics.MetricsAOP;

import java.util.List;
import org.springframework.http.ResponseEntity;
import ro.unibuc.hello.exception.EntityNotFoundException;


import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;

@RestController
@RequestMapping("/friends")
public class FriendRequestController {

    @Autowired
    private FriendRequestService friendRequestService;

    @PostMapping("/send")
    @Timed(value = "friends.send.time", description = "Time taken to send a friend request")
    @Counted(value = "friends.send.count", description = "Number of times sendRequest was called")
    public ResponseEntity<?> sendRequest(@RequestParam String fromUserId, @RequestParam String toUserId) {
        try {
            return ResponseEntity.ok(friendRequestService.sendRequest(fromUserId, toUserId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/requests")
    @Timed(value = "friends.requests.time", description = "Time taken to fetch received friend requests")
    @Counted(value = "friends.requests.count", description = "Number of times getRequests was called")
    public List<FriendRequestEntity> getRequests(@RequestParam String toUserId) {
        return friendRequestService.getReceivedRequests(toUserId);
    }

    @PostMapping("/respond")
    @Timed(value = "friends.respond.time", description = "Time taken to respond to a friend request")
    @Counted(value = "friends.respond.count", description = "Number of times respond was called")
    public String respond(@RequestParam String requestId, @RequestParam boolean accept) {
        boolean success = friendRequestService.respondToRequest(requestId, accept);
        if (success) {
            return accept ? "Friend request accepted." : "Friend request rejected.";
        } else {
            return "Friend request cannot be modified (maybe already responded or not found).";
        }
    }

    @GetMapping("/friends")
    @Timed(value = "friends.list.time", description = "Time taken to fetch friends of a user")
    @Counted(value = "friends.list.count", description = "Number of times getFriends was called")
    public ResponseEntity<?> getFriends(@RequestParam String userId) {
        try {
            return ResponseEntity.ok(friendRequestService.getFriendsOfUser(userId));
        } catch (EntityNotFoundException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    @Timed(value = "friends.remove.time", description = "Time taken to remove a friend")
    @Counted(value = "friends.remove.count", description = "Number of times removeFriend was called")
    public ResponseEntity<String> removeFriend(@RequestParam String userId1, @RequestParam String userId2) {
        boolean removed = friendRequestService.removeFriend(userId1, userId2);
        if (removed) {
            return ResponseEntity.ok("Friend removed successfully.");
        } else {
            return ResponseEntity.badRequest().body("No friendship found between users.");
        }
    }
}



