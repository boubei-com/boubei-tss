/* ==================================================================   
 * Created [2009-7-7] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.framework.persistence.entityaop;

import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.stereotype.Component;

import com.boubei.tss.framework.persistence.IDao;
import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.persistence.ITreeSupportDao;
import com.boubei.tss.util.BeanUtil;

/**
 * entity.decode值维护拦截器.
 */
@Component("decodeInterceptor")
public class DecodeInterceptor extends MatchByDaoMethodNameInterceptor {

    @SuppressWarnings("unchecked")
    public Object invoke(MethodInvocation invocation) throws Throwable {
	    Object target = invocation.getThis();
	    String methodName = invocation.getMethod().getName();
	    
	    switch (judgeManipulateKind(methodName)) {
        case SAVE:
        case UPDATE:
        	IDao<IEntity> dao = (IDao<IEntity>) target;
    		Object[] args = invocation.getArguments();
    		if(args == null || !( BeanUtil.isImplInterface(dao.getType(), IDecodable.class)) ) {
    		    return invocation.proceed();
    		}
    		
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof IDecodable) {
                    IDecodable entity = (IDecodable) args[i];
                    String oldDecode = entity.getDecode();
                    ((ITreeSupportDao<IDecodable>)target).saveDecodeableEntity(entity);
                    
                    /* 移动时维护(修复)所有子节点的decode值。不包括节点自身 
                     * oldDecode == null 说明为新增出来的节点，肯定没有子节点，也就没必要维护子节点了
                     * */
                    if(methodName.startsWith("move") && oldDecode != null && !oldDecode.equals(entity.getDecode())) {
                        repairChildrenDecode(entity, oldDecode, dao);
                    }
                }
            }	
        	break;
        }
        
        return invocation.proceed();
	}
	
    /**
     * 移动时维护所有子节点的decode值.
     * @param entity
     * @param oldDecode
     */
    void repairChildrenDecode(IDecodable entity, String oldDecode, IDao<IEntity> dao){
        String newDecode = entity.getDecode();
 
        List<?> list = dao.getEntities("from " + entity.getClass().getName() + " o where o.decode like ?1", oldDecode + "%" );
        DecodeUtil.repairSubNodeDecode(list, oldDecode, newDecode);
        dao.flush();
    }
}

	