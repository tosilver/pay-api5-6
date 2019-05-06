package co.b4pay.api.common.shiro;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.web.mgt.DefaultWebSubjectFactory;

/**
 * 无状态
 */
public class StatelessDefaultSubjectFactory extends DefaultWebSubjectFactory {

    @Override
    public Subject createSubject(SubjectContext context) {
        // 关闭session的创建
        context.setSessionCreationEnabled(true);
        Subject subject = super.createSubject(context);
        return subject;
    }
}
