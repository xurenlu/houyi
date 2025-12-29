package com.ruoran.houyi.repo;

import com.ruoran.houyi.model.Md5Index;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author renlu
 * created by renlu at 2021/7/15 11:45 上午
 */
@Component
public interface Md5IndexRepo extends CrudRepository<Md5Index, Long>, PagingAndSortingRepository<Md5Index,Long> {
    Optional<Md5Index> findFirstByMd5(String md5);
}
