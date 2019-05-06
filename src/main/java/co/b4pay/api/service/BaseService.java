package co.b4pay.api.service;

import co.b4pay.api.model.base.BaseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.Optional;

/**
 * @author YK
 * @version $Id v 0.1 2017年08月22日 16:36 Exp $
 */
public abstract class BaseService<D extends JpaRepository<T, ID>, T extends BaseEntity, ID extends Serializable> {

    @Autowired
    protected D dao;

    public T getOne(ID id) {
        return dao.getOne(id);
    }

    public T findById(ID id) {
        Optional<T> optional = dao.findById(id);
        return optional.orElse(null);
    }

    public T save(T model) {
        return dao.save(model);
    }

    public void deleteById(ID id) {
        dao.deleteById(id);
    }
}
