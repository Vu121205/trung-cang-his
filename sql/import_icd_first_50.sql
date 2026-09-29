-- Import 50 d?ng ??u ti?n t? DM_ICD.xls v?o b?ng diagnoses.
-- ?nh x?: ICD -> icd_code; MO_TA_BENH_LY -> name; MO_TA_TIENG_ANH -> description.
-- File ???c m? h?a UTF-8. D?ng MySQL 8+.

START TRANSACTION;

INSERT INTO diagnoses (icd_code, name, description, status)
VALUES
    ('V02.0', 'Người đi bộ bị thương do va chạm với xe máy 2 hoặc 3 bánh, tai nạn không do giao thông', 'Pedestrian injured in collision with two- or three-wheeled motor vehicle, nontraffic accident', 'ACTIVE'),
    ('V02.1', 'Người đi bộ bị thương do va chạm với xe máy 2 hoặc 3 bánh, tai nạn giao thông', 'Pedestrian injured in collision with two- or three-wheeled motor vehicle, traffic accident', 'ACTIVE'),
    ('V02.9', 'Người đi bộ bị thương do va chạm với xe máy 2 hoặc 3 bánh, không xác định tai nạn giao thông hay không do giao thông', 'Pedestrian injured in collision with two- or three-wheeled motor vehicle, unspecified whether traffic or nontraffic accident', 'ACTIVE'),
    ('V03', 'Người đi bộ bị thương do va chạm với ô tô, xe bán tải hoặc xe tải nhỏ', 'Pedestrian injured in collision with car, pick-up truck or van', 'ACTIVE'),
    ('V03.0', 'Người đi bộ bị thương do va chạm với ô tô, xe bán tải hoặc xe tải nhỏ, tai nạn không do giao thông', 'Pedestrian injured in collision with car, pick-up truck or van, nontraffic accident', 'ACTIVE'),
    ('T78', 'Tác dụng bất lợi, không phân loại mục khác', 'Adverse effects, not elsewhere classified', 'ACTIVE'),
    ('T78.0', 'Sốc phản vệ do phản ứng dị ứng đối với thực phẩm', 'Anaphylactic shock due to adverse food reaction', 'ACTIVE'),
    ('T78.1', 'Phản ứng có hại khác với thực phẩm, không phân loại mục khác', 'Other adverse food reactions, not elsewhere classified', 'ACTIVE'),
    ('T78.3', 'Phù mạch', 'Angioneurotic oedema', 'ACTIVE'),
    ('T78.4', 'Dị ứng, không xác định', 'Allergy, unspecified', 'ACTIVE'),
    ('T78.8', 'Tác dụng bất lợi khác, không phân loại mục khác', 'Other adverse effects, not elsewhere classified', 'ACTIVE'),
    ('T78.9', 'Tác dụng bất lợi, không xác định', 'Adverse effect, unspecified', 'ACTIVE'),
    ('V14.2', 'Người đi xe đạp bị thương khi va chạm với xe tải hạng nặng hoặc xe buýt, không xác định được vai trò của người bị thương trong tai nạn không do giao thông', 'Pedal cyclist injured in collision with heavy transport vehicle or bus, unspecified pedal cyclist injured in nontraffic accident', 'ACTIVE'),
    ('V14.3', 'Người đi xe đạp bị thương khi va chạm với xe tải hạng nặng hoặc xe buýt, người bị thương khi lên xe hoặc xuống xe', 'Pedal cyclist injured in collision with heavy transport vehicle or bus, person injured while boarding or alighting', 'ACTIVE'),
    ('V14.4', 'Người đi xe đạp bị thương khi va chạm với xe tải hạng nặng hoặc xe buýt, người điều khiển xe bị thương trong tai nạn giao thông', 'Pedal cyclist injured in collision with heavy transport vehicle or bus, driver injured in traffic accident', 'ACTIVE'),
    ('V14.5', 'Người đi xe đạp bị thương khi va chạm với xe tải hạng nặng hoặc xe buýt, người ngồi trên xe bị thương trong tai nạn giao thông', 'Pedal cyclist injured in collision with heavy transport vehicle or bus, passenger injured in traffic accident', 'ACTIVE'),
    ('T88', 'Biến chứng khác của chăm sóc ngoại khoa và/hoặc nội khoa không phân loại mục khác', 'Other complications of surgical and medical care, not elsewhere classified', 'ACTIVE'),
    ('T88.0', 'Nhiễm trùng sau tiêm chủng', 'Infection following immunization', 'ACTIVE'),
    ('T88.1', 'Biến chứng khác sau tiêm chủng, không phân loại mục khác', 'Other complications following immunization, not elsewhere classified', 'ACTIVE'),
    ('T85.5', 'Biến chứng cơ học của thiết bị/dụng cụ nhân tạo, cấy và/hoặc ghép dạ dày - ruột', 'Mechanical complication of gastrointestinal prosthetic devices, implants and grafts', 'ACTIVE'),
    ('T85.6', 'Biến chứng cơ học của thiết bị/dụng cụ nhân tạo xác định khác, cấy và/hoặc ghép bên trong', 'Mechanical complication of other specified internal prosthetic devices, implants and grafts', 'ACTIVE'),
    ('T79', 'Một số biến chứng ban đầu của chấn thương, không phân loại mục khác', 'Certain early complications of trauma, not elsewhere classified', 'ACTIVE'),
    ('T79.0', 'Thuyên tắc khí (do chấn thương)', 'Air embolism (traumatic)', 'ACTIVE'),
    ('T79.1', 'Thuyên tắc mỡ (do chấn thương)', 'Fat embolism (traumatic)', 'ACTIVE'),
    ('T79.2', 'Xuất huyết thứ phát và/hoặc tái phát do chấn thương', 'Traumatic secondary and recurrent haemorrhage', 'ACTIVE'),
    ('T79.3', 'Nhiễm trùng vết thương sau chấn thương, không phân loại mục khác', 'Post-traumatic wound infection, not elsewhere classified', 'ACTIVE'),
    ('T73.9', 'Tác động của thiếu hút, không xác định', 'Effect of deprivation, unspecified', 'ACTIVE'),
    ('T74', 'Hội chứng ngược đãi', 'Maltreatment syndromes', 'ACTIVE'),
    ('T97', 'Di chứng của tác động độc hại do chất chủ yếu có nguồn gốc không phải là thuốc', 'Sequelae of toxic effects of substances chiefly nonmedicinal as to source', 'ACTIVE'),
    ('T98', 'Di chứng do tác động từ nguyên nhân bên ngoài khác và/hoặc không xác định', 'Sequelae of other and unspecified effects of external causes', 'ACTIVE'),
    ('T98.0', 'Di chứng do tác động của dị vật xâm nhập qua lỗ tự nhiên', 'Sequelae of effects of foreign body entering through natural orifice', 'ACTIVE'),
    ('V14.9', 'Người đi xe đạp bị thương khi va chạm với xe tải hạng nặng hoặc xe buýt, không xác định được vai trò của người bị thương trong tai nạn giao thông', 'Pedal cyclist injured in collision with heavy transport vehicle or bus, unspecified pedal cyclist injured in traffic accident', 'ACTIVE'),
    ('V15', 'Người đi xe đạp bị thương khi va chạm với tàu hỏa hoặc phương tiện đường sắt', 'Pedal cyclist injured in collision with railway train or railway vehicle', 'ACTIVE'),
    ('V15.0', 'Người đi xe đạp bị thương khi va chạm với tàu hỏa hoặc phương tiện đường sắt, người điều khiển xe bị thương trong tai nạn không do giao thông', 'Pedal cyclist injured in collision with railway train or railway vehicle, driver injured in nontraffic accident', 'ACTIVE'),
    ('V13.1', 'Người đi xe đạp bị thương khi va chạm với ô tô, xe bán tải hoặc xe tải nhỏ, người ngồi trên xe bị thương trong tai nạn không do giao thông', 'Pedal cyclist injured in collision with car, pick-up truck or van, passenger injured in nontraffic accident', 'ACTIVE'),
    ('T85.7', 'Nhiễm trùng và/hoặc phản ứng viêm do thiết bị/dụng cụ nhân tạo khác, cấy và/hoặc ghép bên trong', 'Infection and inflammatory reaction due to other internal prosthetic devices, implants and grafts', 'ACTIVE'),
    ('T85.8', 'Biến chứng khác của thiết bị/dụng cụ nhân tạo, cấy và/hoặc ghép bên trong, không phân loại mục khác', 'Other complications of internal prosthetic devices, implants and grafts, not elsewhere classified', 'ACTIVE'),
    ('T85.9', 'Biến chứng không xác định của thiết bị/dụng cụ nhân tạo cấy và/hoặc ghép bên trong', 'Unspecified complication of internal prosthetic device, implant and graft', 'ACTIVE'),
    ('T86', 'Thất bại và/hoặc thải ghép tạng và/hoặc mô', 'Failure and rejection of transplanted organs and tissues', 'ACTIVE'),
    ('T86.0', 'Thải ghép tủy xương', 'Bone-marrow transplant rejection', 'ACTIVE'),
    ('T74.0', 'Thờ ơ hoặc bỏ rơi', 'Neglect or abandonment', 'ACTIVE'),
    ('T74.1', 'Bạo hành thể xác', 'Physical abuse', 'ACTIVE'),
    ('T74.2', 'Lạm dụng tình dục', 'Sexual abuse', 'ACTIVE'),
    ('T74.3', 'Thao túng tâm lý', 'Psychological abuse', 'ACTIVE'),
    ('T74.8', 'Hội chứng ngược đãi khác', 'Other maltreatment syndromes', 'ACTIVE'),
    ('T74.9', 'Hội chứng ngược đãi, không xác định', 'Maltreatment syndrome, unspecified', 'ACTIVE'),
    ('T75', 'Tác động của nguyên nhân bên ngoài khác', 'Effects of other external causes', 'ACTIVE'),
    ('T75.0', 'Tác động của sét/chớp', 'Effects of lightning', 'ACTIVE'),
    ('T98.1', 'Di chứng do tác động từ nguyên nhân bên ngoài khác và/hoặc không xác định', 'Sequelae of other and unspecified effects of external causes', 'ACTIVE'),
    ('T98.2', 'Di chứng của một số biến chứng sớm của chấn thương', 'Sequelae of certain early complications of trauma', 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

COMMIT;


