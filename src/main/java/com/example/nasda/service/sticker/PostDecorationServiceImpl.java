package com.example.nasda.service.sticker;

import com.example.nasda.domain.*;
import com.example.nasda.dto.sticker.PostDecorationRequestDTO;
import com.example.nasda.dto.sticker.PostDecorationResponseDTO;
import com.example.nasda.repository.PostImageRepository;
import com.example.nasda.repository.sticker.PostDecorationRepository;
import com.example.nasda.repository.sticker.StickerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostDecorationServiceImpl implements PostDecorationService {

    private final PostDecorationRepository postDecorationRepository;
    private final PostImageRepository postImageRepository;
    private final StickerRepository stickerRepository;
    private final UserRepository userRepository;
    // private final NotificationService notificationService; // 알림 서비스가 있다고 가정

    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public List<PostDecorationResponseDTO> saveDecorations(PostDecorationRequestDTO requestDTO) {

        // 1. 도배 방지 (Throttle): 특정 사용자가 한 이미지에 붙인 장식 개수 제한
        long currentCount = postDecorationRepository.countByUser_UserIdAndPostImage_ImageId(
                requestDTO.getUserId(), requestDTO.getPostImageId());

        if (currentCount + requestDTO.getDecorations().size() > 50) { // 예: 최대 50개 제한
            throw new IllegalStateException("한 이미지에 더 이상 스티커를 붙일 수 없습니다. (최대 50개)");
        }

        // 2. 공통 정보 조회
        PostImageEntity postImage = postImageRepository.findById(requestDTO.getPostImageId())
                .orElseThrow(() -> new IllegalArgumentException("이미지 없음"));
        UserEntity decorator = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // 3. 성능 최적화: findAllById를 사용하여 스티커 한 번에 가져오기
        List<Integer> stickerIds = requestDTO.getDecorations().stream()
                .map(PostDecorationRequestDTO.DecorationItem::getStickerId)
                .distinct()
                .collect(Collectors.toList());

        List<StickerEntity> stickerEntities = stickerRepository.findAllById(stickerIds);

        // 빠른 매핑을 위해 Map으로 변환
        Map<Integer, StickerEntity> stickerMap = stickerEntities.stream()
                .collect(Collectors.toMap(StickerEntity::getStickerId, s -> s));

        // 4. 엔티티 변환 및 저장
        List<PostDecorationEntity> entities = requestDTO.getDecorations().stream()
                .map(item -> {
                    StickerEntity sticker = stickerMap.get(item.getStickerId());
                    if (sticker == null) throw new IllegalArgumentException("존재하지 않는 스티커 포함");

                    return PostDecorationEntity.builder()
                            .post(postImage.getPost())
                            .postImage(postImage)
                            .user(decorator)
                            .sticker(sticker)
                            .posX(item.getPosX())
                            .posY(item.getPosY())
                            .scale(item.getScale())
                            .rotation(item.getRotation())
                            .zIndex(item.getZIndex())
                            .build();
                })
                .collect(Collectors.toList());

        List<PostDecorationEntity> savedEntities = postDecorationRepository.saveAll(entities);

        // 5. 알림 로직: 게시글 원작자에게 알림 발송
        UserEntity postOwner = postImage.getPost().getUser();
        if (!postOwner.getUserId().equals(decorator.getUserId())) {
            log.info("알림 발송: {}님이 {}님의 사진을 꾸몄습니다!", decorator.getNickname(), postOwner.getNickname());
            // notificationService.send(postOwner, decorator.getNickname() + "님이 당신의 사진을 이쁘게 꾸며주셨어요! ✨");
        }

        return savedEntities.stream()
                .map(entity -> modelMapper.map(entity, PostDecorationResponseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<PostDecorationResponseDTO> getDecorationsByImageId(Integer imageId) {
        log.info("🔍 [꾸미기 조회] 이미지 ID={} 에 부착된 스티커 목록을 가져옵니다.", imageId);

        // 1. 리포지토리를 통해 해당 이미지의 장식 엔티티들을 조회
        // (리포지토리에 @EntityGraph를 설정했으므로 Sticker 정보도 한 번에 가져옵니다)
        List<PostDecorationEntity> entities = postDecorationRepository.findByPostImage_ImageId(imageId);

        // 2. 엔티티 리스트를 ResponseDTO 리스트로 변환하여 반환
//        return entities.stream()
//                .map(entity -> modelMapper.map(entity, PostDecorationResponseDTO.class))
//                .collect(Collectors.toList());
        return entities.stream()
                .map(entity -> PostDecorationResponseDTO.from(entity))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateDecoration(Integer decorationId, PostDecorationRequestDTO.DecorationItem updateDTO, Integer currentUserId) {
        // 1. 기존 장식 조회
        PostDecorationEntity decoration = postDecorationRepository.findById(decorationId)
                .orElseThrow(() -> new IllegalArgumentException("수정할 장식이 존재하지 않습니다."));

        // 2. 권한 체크: 스티커를 붙인 본인인지 확인
        if (!decoration.getUser().getUserId().equals(currentUserId)) {
            throw new SecurityException("자신이 붙인 스티커만 수정할 수 있습니다.");
        }

        // 3. 데이터 갱신 (Dirty Checking 활용)
        // Repository 테스트 때 언급했듯이 엔티티에 changePosition 같은 메서드가 있다면 호출하고,
        // 없다면 리포지토리의 @Query 업데이트 메서드를 사용합니다.
        decoration.changePosition(
                updateDTO.getPosX(),
                updateDTO.getPosY(),
                updateDTO.getScale(),
                updateDTO.getRotation()
        );

        log.info("장식 수정 완료: ID={}, 새로운 위치={},{}", decorationId, updateDTO.getPosX(), updateDTO.getPosY());
    }

    @Override
    @Transactional
    public void deleteDecoration(Integer decorationId, Integer currentUserId) {
        PostDecorationEntity decoration = postDecorationRepository.findById(decorationId)
                .orElseThrow(() -> new IllegalArgumentException("장식 없음"));

        // 삭제 권한 체크: 스티커를 붙인 본인이거나, 게시글의 주인이어야 함
        Integer decoratorId = decoration.getUser().getUserId();
        Integer postOwnerId = decoration.getPost().getUser().getUserId();

        if (currentUserId.equals(decoratorId) || currentUserId.equals(postOwnerId)) {
            postDecorationRepository.delete(decoration);
            log.info("장식 삭제 성공: ID {}", decorationId);
        } else {
            throw new SecurityException("삭제 권한이 없습니다.");
        }
    }

    @Override
    public List<PostDecorationResponseDTO> getDecorationsByPostId(Integer postId) {
        log.info("🔍 [게시글 전체 조회] 게시글 ID={} 의 모든 스티커를 로드합니다.", postId);

        // 1. 이미 작성하신 Repository의 findByPostPostId 메서드를 활용합니다.
        // 인자 타입이 Integer이므로 Long.valueOf 없이 바로 사용 가능합니다.
        List<PostDecorationEntity> entities = postDecorationRepository.findByPostPostId(postId);

        return entities.stream()
                .map(PostDecorationResponseDTO::from)
                .collect(Collectors.toList());
    }

}