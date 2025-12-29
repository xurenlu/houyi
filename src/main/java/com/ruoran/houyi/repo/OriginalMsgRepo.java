package com.ruoran.houyi.repo;

import com.ruoran.houyi.model.OriginalMsg;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author renlu
 * created by renlu at 2021/7/14 5:45 下午
 */
@Component
public interface OriginalMsgRepo extends CrudRepository<OriginalMsg, Long>, PagingAndSortingRepository<OriginalMsg,Long>  {
        public Optional<OriginalMsg> findFirstByCorpIdAndMsgIdAndSeq(String corpId,String msgId,long seq);


        /**
         * 删除两天前的消息
         */
        @Modifying
        @Query(value ="delete from original_msg where `date_no` <= date_format(DATE_SUB(now(),INTERVAL 2 day),'%Y%m%d') limit 3000",nativeQuery = true )
        void deleteOldMsgs();

        @Query(value ="select * from original_msg where `date_no` >= date_format( date_sub(now(),INTERVAL  1 day ),'%Y%m%d') and msg_type NOT in ('revoke','text','location','agree','disagree','weapp','card','todo','collect','redpacket','docmsg','markdown','calendar','news','external_redpacket','sphfeed','link','meeting') and  (`push_at`  is null or `push_at`  between -9 and 0)  order by push_at desc  ,id asc  limit 500",nativeQuery =true)
        List<OriginalMsg> findNotPushMessage();

        @Query(value ="select * from original_msg where `date_no` >= date_format( date_sub(now(),INTERVAL  1 day ),'%Y%m%d') and  `push_at`  <= -999  order by push_at desc,id limit 30",nativeQuery =true)
        List<OriginalMsg> findBigFileNotPushMessage();
}
