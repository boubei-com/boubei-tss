package com.boubei.tss.framework.sms;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.boubei.tss.cache.Cacheable;
import com.boubei.tss.cache.Pool;
import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.log.BusinessLogger;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;

public abstract class AbstractSMS {

    private Logger log = Logger.getLogger(this.getClass());

    protected ICommonService commService;
    protected String domain;

    /**
     * 异常信息转换器实现类类名属性名
     */
    static final String SMS_CLASS_NAME = "class.name.SMS";

    private static Pool cache = CacheHelper.getLongCache();

    public static AbstractSMS create(String domain, String smsKey, String smsSecret, String smsSign) {
        String key = domain + smsKey + smsSecret + smsSign + "_sms";
        Cacheable cacheItem = cache.getObject(key);
        if (cacheItem == null) {
            String className = ParamConfig.getAttribute(SMS_CLASS_NAME, DefaultSMS.class.getName());
            Object instance;
            if (smsKey != null) {
                instance = BeanUtil.newInstanceByName(className
                        , new Class[]{String.class, String.class, String.class}
                        , new String[]{smsKey, smsSecret, smsSign});
            } else {
                instance = BeanUtil.newInstanceByName(className, new Class[]{String.class}, new String[]{domain});
            }
            cacheItem = cache.putObject(key, instance);
        }

        AbstractSMS sms = (AbstractSMS) cacheItem.getValue();
        sms.domain = domain;
        return sms;
    }

    public static AbstractSMS create() {
        return create(Environment.getDomain(), null, null, null);
    }

    /**
     * 按照短信模板发送短信
     *
     * @param phone
     * @param tlCode  模板编号
     * @param tlParam {“code”:”1234”,”product”:”ytx”}
     * @param outId   外部流水ID
     * @return
     */
    public abstract Object send(String phone, String tlCode, String tlParam, Object outId);

    public abstract Object send(String sign, String phone, String tlCode, String tlParam, Object outId);

    /**
     * 发送随机数验证码
     *
     * @param phone
     * @return
     */
    public abstract Object sendRandomNum(String phone);

    /**
     * 验证短信验证码是否有效
     *
     * @param mobile
     * @param code   : WMS/E8V2 = 833834
     * @return
     */
    public boolean checkCode(String mobile, Object code) {
        try {
            code = EasyUtils.obj2Int(code);
        } catch (Exception e) {
            code = -99999;
        }

        Date from = DateUtil.subDays(new Date(), 30.0 / (24 * 60));
        String hql = " from SMSLog where randomnum = ?1 and ((phonenum = ?2  and createTime > ?3) or creator='SMS_Admin') ";
        List<?> list = commService.getList(hql, code, mobile, from);

        return list.size() > 0;
    }

    // 发送不成功则记录异常
    protected void logException(Exception e) {
        String errMsg = "短信接口异常";
        log.error(errMsg, e);
        BusinessLogger.log(errMsg, e.getMessage(), e.getCause());
    }

    /**
     * 大陆手机号码11位数，匹配格式.
     */
    public static boolean isChinaPhoneLegal(String phone) {
        if (EasyUtils.isNullOrEmpty(phone)) return false;

        String regExp = "^[1][3,4,5,6,7,8,9][0-9]{9}$";
        Pattern p = Pattern.compile(regExp);
        Matcher m = p.matcher(phone);
        return m.matches();
    }
}
