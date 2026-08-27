-- Explore 页面模板帖子种子数据。请先执行 social_migration.sql
USE travel_agent;

INSERT INTO social_note (user_id,title,content,cover_url,destination,tags,author_name,visibility,status,like_count)
SELECT * FROM (
  SELECT '75476126' AS user_id,'杭州 3 日慢旅行｜把西湖留给清晨' AS title,'Day 1：抵达后入住西湖边的民宿，清晨错峰逛苏堤。\nDay 2：灵隐寺 + 龙井村徒步，感受茶山。\nDay 3：九溪烟树慢走，再喝一杯桂花龙井收尾。' AS content,'https://images.unsplash.com/photo-1536599018102-9f803c3e0a2a?auto=format&fit=crop&w=900&q=80' AS cover_url,'杭州' AS destination,JSON_ARRAY('轻松漫游','人文') AS tags,'Kao' AS author_name,'public' AS visibility,'published' AS status,2400 AS like_count
  UNION ALL SELECT '75476126','珠海周末不踩雷：海边、老街和一顿好吃的','Day 1：情侣路骑车看海，傍晚去湾仔吃生蚝。\nDay 2：唐家湾老街散步，挖掘本地糖水铺与咖啡店。','https://images.unsplash.com/photo-1507521292222-0f3f9d4e5d3d?auto=format&fit=crop&w=900&q=80','珠海',JSON_ARRAY('周末','美食'),'Momo','public','published',1800
  UNION ALL SELECT '75476126','川西 7 天｜把海拔和体力写进计划','Day 1：成都出发，适应海拔，住康定。\nDay 2-3：新都桥、塔公草原，边走边拍。\nDay 4-5：稻城亚丁深度徒步。\nDay 6-7：回程，留缓冲应对高反。','https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=900&q=80','川西',JSON_ARRAY('自驾','户外'),'山野来信','public','published',986
  UNION ALL SELECT '75476126','第一次京都，住哪里才能少走回头路？','建议住在京都站或四条附近，交通便利。\nDay 1：清水寺、二年坂、三年坂。\nDay 2：伏见稻荷大社 + 宇治抹茶。\nDay 3：岚山竹林与保津川。','https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&w=900&q=80','京都',JSON_ARRAY('日本','路线'),'Nana','public','published',3100
  UNION ALL SELECT '75476126','北京故宫深度游｜避开人流的路线','建议工作日早 8 点入场，避开旅行团。路线：午门 → 太和殿 → 中和殿 → 保和殿 → 乾清宫 → 御花园 → 神武门。重点看珍宝馆和钟表馆。','https://images.unsplash.com/photo-1508804185872-d7badad00f7d?auto=format&fit=crop&w=900&q=80','北京',JSON_ARRAY('历史','路线'),'旅人阿福','public','published',1500
  UNION ALL SELECT '75476126','江西萍乡武功山｜云端徒步','Day 1：沈子村上山，住金顶帐篷。\nDay 2：看日出后发云界徒步至明月山。全程约 18 公里，建议 2 天行程。','https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=900&q=80','萍乡',JSON_ARRAY('徒步','户外'),'山友日记','public','published',756
  UNION ALL SELECT '75476126','珠海日月贝大剧院周边打卡','傍晚日落时分最佳，贝壳建筑在夕阳下非常出片。周边可逛：野狸岛公园、情侣路、香洲湾。美食推荐：湾仔海鲜街、官也街。','https://images.unsplash.com/photo-1589394815804-964ed0be2eb5?auto=format&fit=crop&w=900&q=80','珠海',JSON_ARRAY('打卡','海边'),'湾仔吃货','public','published',1200
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM social_note n WHERE n.title=seed.title AND n.user_id=seed.user_id);

INSERT INTO inspiration (user_id,name,quote,description,tags,best_season,status,priority,sort_order)
SELECT '75476126','珠海日月贝大剧院','日月贝，怎么看都有面！','傍晚去野狸岛看日落，顺路逛情侣路。',JSON_ARRAY('夏季'),'春夏秋',1,0,10
WHERE NOT EXISTS (SELECT 1 FROM inspiration WHERE user_id='75476126' AND name='珠海日月贝大剧院');
INSERT INTO inspiration (user_id,name,quote,description,tags,best_season,status,priority,sort_order)
SELECT '75476126','北京','想去故宫解开尘封几千年的秘密','避开节假日人流，预留半天参观珍宝馆。',JSON_ARRAY('旅行灵感'),'春秋',1,0,9
WHERE NOT EXISTS (SELECT 1 FROM inspiration WHERE user_id='75476126' AND name='北京');
