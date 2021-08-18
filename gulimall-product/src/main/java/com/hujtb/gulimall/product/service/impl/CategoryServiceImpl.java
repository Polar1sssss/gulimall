package com.hujtb.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hujtb.gulimall.product.service.CategoryBrandRelationService;
import com.hujtb.gulimall.product.vo.Catalog2Vo;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hujtb.common.utils.PageUtils;
import com.hujtb.common.utils.Query;

import com.hujtb.gulimall.product.dao.CategoryDao;
import com.hujtb.gulimall.product.entity.CategoryEntity;
import com.hujtb.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redisson;

    @Override
    public void removeMenusByIds(List<Long> asList) {
        // TODO 1、检查当前删除菜单是否被其他地方引用
        baseMapper.deleteBatchIds(asList);
    }

    /**
     * 根据catalogId查出完整路径
     *
     * @param catalogId
     * @return
     */
    @Override
    public Long[] findPathById(Long catalogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(paths, catalogId);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     *  
     * @param category
     */
//    @Caching(evict = {
//            @CacheEvict(value = "category", key = "'getLevel1Categories'"),
//            @CacheEvict(value = "category", key = "'getCatalogJson'")
//    })
    @CacheEvict(value = "category", allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * 查询1级分类
     *
     * @return
     * @Cacheable：代表当前方法的返回结果需要缓存，如果缓存中有，方法不会被调用。如果缓存中没有，会调用方法，最后将方法返回结果放到缓存 每个需要缓存的数据都需要指定分区（按照业务类型）
     * key默认自动生成：缓存的名字::SimpleKey[]（自助生成的key值）
     * 缓存的value值，默认使用jdk序列化机制，将序列化后的数据存入redis
     * 默认时间是-1
     */
    @Cacheable(value = "category", key = "#root.method.name")
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        List<CategoryEntity> entities = this.baseMapper.selectList(
                new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return entities;
    }

    @Cacheable(value = "category", key = "#root.methodName")
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        // 查出所有一级分类
        List<CategoryEntity> level1Categories = this.getParentCid(categoryEntities, 0L);
        Map<String, List<Catalog2Vo>> parent_cid = level1Categories.stream().collect(
                Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    // 根据一级分类id查出所有的二级分类
                    List<CategoryEntity> entities = getParentCid(categoryEntities, v.getCatId());
                    List<Catalog2Vo> catalog2Vos = null;
                    if (entities != null) {
                        // 封装Catalog2Vo对象成为一个list
                        catalog2Vos = entities.stream().map(l2 -> {
                            Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            // 根据二级分类id查出所有的三级分类
                            List<CategoryEntity> entities1 = getParentCid(categoryEntities, l2.getCatId());
                            List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                            if (entities1 != null) {
                                // 封装Catalog3Vo对象成为一个list
                                catalog3Vos = entities1.stream().map(l3 -> {
                                    Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(catalog2Vo.getId().toString(), l3.getCatId().toString(), l3.getName());
                                    return catalog3Vo;
                                }).collect(Collectors.toList());
                            }
                            // 为Catalog2Vo对象设置Catalog3Vo属性
                            catalog2Vo.setCatalog3List(catalog3Vos);
                            return catalog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return catalog2Vos;
                }));
        return parent_cid;
    }

    /**
     * 缓存三级分类数据
     *
     * @return
     */
    // TODO 压力测试：产生堆外内存溢出：OutOfDirectMemoryError
    // 1）SpringBoot2.0之后使用lettuce作为操作redis客户端，它使用netty进行网络通信
    // 2）lettuce的bug导致netty堆外内存溢出，netty如果没有指定堆外内存，会使用参数中的-Xmx100m，可以通过-Dio.netty.maxDirectMemory设置
    // 解决方案：不能单纯通过设置-Dio.netty.maxDirectMemory解决上述问题
    // 1）升级lettuce客户端 2）切换使用jedis
    public Map<String, List<Catalog2Vo>> getCatalogJson2() {
        // 加入缓存逻辑，缓存中存放的json字符串，json是跨语言跨平台兼容的
        String catalogJson = redisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJson)) {
            // 缓存中没有，查询数据库
            Map<String, List<Catalog2Vo>> catalogJsonFromDb = getCatalogJsonFromDb();
            return catalogJsonFromDb;
        }
        Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {
        });
        return result;
    }

    /**
     * 从数据库查询三级分类数据
     * 使用redisson
     * List<Catalog3Vo> -> List<Catalog2Vo> -> Map<String, List<Catalog2Vo>>
     *
     * @return
     */
    private Map<String, List<Catalog2Vo>> getCatalogJsonFromDb() {
        // 封装成map数据
        Map<String, List<Catalog2Vo>> parent_cid;
        // 防止缓存击穿，需要在读取数据库时加分布式锁
        RLock lock = redisson.getLock("catalogJson-lock");
        lock.lock();
        try {
            List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
            // 查出所有一级分类
            List<CategoryEntity> level1Categories = this.getParentCid(categoryEntities, 0L);
            parent_cid = level1Categories.stream().collect(
                    Collectors.toMap(k -> k.getCatId().toString(), v -> {
                        // 根据一级分类id查出所有的二级分类
                        List<CategoryEntity> entities = getParentCid(categoryEntities, v.getCatId());
                        List<Catalog2Vo> catalog2Vos = null;
                        if (entities != null) {
                            // 封装Catalog2Vo对象成为一个list
                            catalog2Vos = entities.stream().map(l2 -> {
                                Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                                // 根据二级分类id查出所有的三级分类
                                List<CategoryEntity> entities1 = getParentCid(categoryEntities, l2.getCatId());
                                List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                                if (entities1 != null) {
                                    // 封装Catalog3Vo对象成为一个list
                                    catalog3Vos = entities1.stream().map(l3 -> {
                                        Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(catalog2Vo.getId().toString(), l3.getCatId().toString(), l3.getName());
                                        return catalog3Vo;
                                    }).collect(Collectors.toList());
                                }
                                // 为Catalog2Vo对象设置Catalog3Vo属性
                                catalog2Vo.setCatalog3List(catalog3Vos);
                                return catalog2Vo;
                            }).collect(Collectors.toList());
                        }
                        return catalog2Vos;
                    }));
            String s = JSON.toJSONString(parent_cid);
            redisTemplate.opsForValue().set("catalogJSON", s, Duration.ofDays(1));
        } finally {
            lock.unlock();
        }
        return parent_cid;
    }

    /**
     * 获取父类id
     *
     * @param categoryEntities
     * @param parentCid
     * @return
     */
    private List<CategoryEntity> getParentCid(List<CategoryEntity> categoryEntities, Long parentCid) {
        List<CategoryEntity> collect = categoryEntities.stream()
                .filter(item -> item.getParentCid() == parentCid).collect(Collectors.toList());
        return collect;
    }

    /**
     * 查找当前节点的父id
     *
     * @param paths
     * @param currentId
     * @return
     */
    private List<Long> findParentPath(List<Long> paths, Long currentId) {
        paths.add(currentId);
        CategoryEntity byId = this.getById(currentId);
        if (byId.getParentCid() != 0) {
            findParentPath(paths, byId.getParentCid());
        }
        return paths;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 查询所有分类并组成树状结构
     *
     * @return
     */
    @Override
    public List<CategoryEntity> queryWithTree() {
        // 查询所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        // 查询出1级分类
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter(
                categoryEntity -> categoryEntity.getParentCid() == 0
        ).map((menu) -> {
            menu.setChildren(getChildrens(menu, categoryEntities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return level1Menus;
    }

    /**
     * 递归查找当前菜单的子菜单
     *
     * @param root 当前分类
     * @param all  所有分类
     * @return
     */
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> childrenList = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid().equals(root.getCatId());
        }).map(categoryEntity -> {
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return childrenList;
    }


}