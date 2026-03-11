package io.attestry.userauth.infrastructure.persistence.jpa.onboarding;

import io.attestry.userauth.application.port.onboarding.OnboardingEvidenceBundleRepositoryPort;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceBundle;
import io.attestry.userauth.domain.onboarding.model.OnboardingEvidenceFile;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.OnboardingEvidenceBundleJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.mapper.OnboardingEvidenceBundleMapper;
import io.attestry.userauth.infrastructure.persistence.jpa.mapper.OnboardingEvidenceFileMapper;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OnboardingEvidenceBundleJpaRepository;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.OnboardingEvidenceFileJpaRepository;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class JpaOnboardingEvidenceBundleRepositoryAdapter
    implements OnboardingEvidenceBundleRepositoryPort {

    private final OnboardingEvidenceBundleJpaRepository bundleRepository;
    private final OnboardingEvidenceFileJpaRepository fileRepository;
    private final OnboardingEvidenceBundleMapper bundleMapper;

    @Override
    public OnboardingEvidenceBundle save(OnboardingEvidenceBundle bundle) {
        bundleRepository.save(bundleMapper.toEntity(bundle));
        OnboardingEvidenceFileMapper fileMapper = bundleMapper.fileMapper();
        for (OnboardingEvidenceFile file : bundle.files()) {
            fileRepository.save(fileMapper.toEntity(file));
        }
        return bundle;
    }

    @Override
    public Optional<OnboardingEvidenceBundle> findById(String evidenceBundleId) {
        return bundleRepository.findById(evidenceBundleId).map(entity -> {
            List<OnboardingEvidenceFile> files = bundleMapper.fileMapper()
                .toDomainList(fileRepository.findByEvidenceBundleId(evidenceBundleId));
            return bundleMapper.toDomain(entity, files);
        });
    }

    @Override
    public List<OnboardingEvidenceBundle> findByIds(List<String> evidenceBundleIds) {
        if (evidenceBundleIds == null || evidenceBundleIds.isEmpty()) {
            return List.of();
        }

        List<OnboardingEvidenceBundleJpaEntity> bundles = bundleRepository.findAllById(evidenceBundleIds);
        OnboardingEvidenceFileMapper fileMapper = bundleMapper.fileMapper();
        Map<String, List<OnboardingEvidenceFile>> filesByBundleId =
            fileRepository.findByEvidenceBundleIdIn(evidenceBundleIds).stream()
                .map(fileMapper::toDomain)
                .collect(Collectors.groupingBy(
                    OnboardingEvidenceFile::evidenceBundleId,
                    LinkedHashMap::new,
                    Collectors.toList()
                ));

        return bundles.stream()
            .map(bundle -> bundleMapper.toDomain(
                bundle,
                filesByBundleId.getOrDefault(bundle.getEvidenceBundleId(), Collections.emptyList())
            ))
            .toList();
    }

    @Override
    public Optional<OnboardingEvidenceFile> findFileById(String evidenceFileId) {
        return fileRepository.findById(evidenceFileId).map(bundleMapper.fileMapper()::toDomain);
    }
}
