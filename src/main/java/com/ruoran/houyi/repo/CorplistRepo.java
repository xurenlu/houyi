package com.ruoran.houyi.repo;

import com.ruoran.houyi.model.CorpInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author renlu
 * created by renlu at 2021/7/14 5:29 下午
 */
@Component
public interface CorplistRepo extends CrudRepository<CorpInfo, Long>, PagingAndSortingRepository<CorpInfo,Long> {
    List<CorpInfo> findAll();
    List<CorpInfo> findAllByStatus(long status);

    Optional<CorpInfo> findFirstByCorpid(String corpId);
}
