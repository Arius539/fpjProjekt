package org.fpj.wall.application;

import org.fpj.wall.domain.WallComment;
import org.fpj.wall.domain.WallCommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WallCommentService {

    private final WallCommentRepository wallCommentRepo;

    @Autowired
    public WallCommentService(WallCommentRepository wallCommentRepo){
        this.wallCommentRepo = wallCommentRepo;
    }

    public Page<WallComment> getWallCommentsByAuthor(long userId,PageRequest pageRequest){
        return wallCommentRepo.findByAuthor_IdOrderByCreatedAtDesc(userId,pageRequest);
    }

    public Page<WallComment> getWallCommentsCreatedByWallOwner(long userId,PageRequest pageRequest){
        return wallCommentRepo.findByWallOwner_IdOrderByCreatedAtDesc(userId,pageRequest);
    }

    public WallComment add(WallComment comment){
        if(comment.getWallOwner() == null)throw new  IllegalArgumentException("Es ist ein Fehler beim laden der nötigen Informationen aufgetreten");
        if(comment.getAuthor() == null)throw new  IllegalArgumentException("Es ist ein Fehler beim laden der nötigen Informationen aufgetreten");
        if(comment.getWallOwner().getUsername().equals(comment.getAuthor().getUsername()))throw new  IllegalArgumentException("Du kannst nicht auf deiner eigenen Pinnwand kommentieren");

        wallCommentRepo.save(comment);
        return comment;
    }

    public List<WallComment> toListByAuthor(Long authorId) {
        return wallCommentRepo.toListByAuthor(authorId);
    }

    public List<WallComment> toListByWallOwner(Long ownerId) {
        return wallCommentRepo.toListByWallOwner(ownerId);
    }

}
