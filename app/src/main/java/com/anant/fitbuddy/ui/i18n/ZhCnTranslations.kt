package com.anant.fitbuddy.ui.i18n

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString

/**
 * Simplified Chinese (`zh-CN`) translation dictionary and dynamic pattern rules for FitBuddy.
 */
object ZhCnTranslations {

    fun translate(text: String): String {
        exactMap[text]?.let { return it }
        val trimmed = text.trim()
        if (trimmed != text) {
            exactMap[trimmed]?.let { translated ->
                val leading = text.takeWhile { it.isWhitespace() }
                val trailing = text.takeLastWhile { it.isWhitespace() }
                return leading + translated + trailing
            }
        }
        return translateDynamic(text)
    }

    fun translateAnnotated(text: AnnotatedString): AnnotatedString {
        val raw = text.text
        if (raw == "Crafted with ❤️ by Anant") {
            val anantStyle = text.spanStyles.firstOrNull {
                raw.substring(it.start, it.end) == "Anant"
            }?.item
            return buildAnnotatedString {
                append("用 ❤️ 精心打造 · 作者 ")
                if (anantStyle != null) {
                    pushStyle(anantStyle)
                    append("Anant")
                    pop()
                } else {
                    append("Anant")
                }
            }
        }
        exactMap[raw]?.let { translated ->
            if (text.spanStyles.isEmpty()) return AnnotatedString(translated)
        }
        return text
    }

    private val exactMap: Map<String, String> = buildMap(2400) {
        // --- Language / i18n ---
        put("Language", "语言 / Language")
        put("Internationalization", "国际化语言")
        put("App language", "应用语言")
        put("Choose the display language for FitBuddy. System default follows your device language.", "选择 FitBuddy 的界面显示语言。“系统默认”将跟随设备系统语言。")
        put("Follows device language (Simplified Chinese or English).", "跟随系统语言（支持简体中文或英文）。")
        put("System default", "系统默认")
        put("English", "英文")
        put("简体中文", "简体中文")
        put("Chinese (Simplified)", "简体中文")

        // --- Bottom Bar & Top Navigation ---
        put("Today", "今日")
        put("Yesterday", "昨天")
        put("Week", "周视图")
        put("Progress", "趋势")
        put("Body", "身体")
        put("Settings", "设置")
        put("Log", "记录")
        put("Back", "返回")
        put("Next", "下一步")
        put("Close", "关闭")
        put("Cancel", "取消")
        put("Save", "保存")
        put("Done", "完成")
        put("Delete", "删除")
        put("Edit", "编辑")
        put("Confirm", "确认")
        put("Continue", "继续")
        put("Skip", "跳过")
        put("Retry", "重试")
        put("Clear", "清除")
        put("Reset", "重置")
        put("Copy", "复制")
        put("Share", "分享")
        put("Update", "更新")
        put("Apply", "应用")
        put("Dismiss", "关闭")
        put("Got it", "知道了")
        put("Not now", "暂不")
        put("Yes", "是")
        put("No", "否")
        put("OK", "确定")
        put("Search", "搜索")
        put("All", "全部")
        put("Optional", "选填")
        put("Required", "必填")
        put("Loading…", "加载中…")
        put("Saving…", "保存中…")
        put("Validating…", "验证中…")
        put("Restoring…", "恢复中…")
        put("Uploading…", "上传中…")
        put("Downloading…", "下载中…")

        // --- Greetings & Dashboard ---
        put("Dashboard", "今日总览")
        put("Week history", "本周记录")
        put("Good morning", "早上好")
        put("Good afternoon", "下午好")
        put("Good evening", "晚上好")
        put("What's your name?", "如何称呼你？")
        put("Used for a friendly greeting on your dashboard.", "用于在首页向你展示亲切的问候。")
        put("First name", "名字 / 昵称")
        put("Last name (optional)", "姓氏（选填）")
        put("Last name", "姓氏")
        put("Analyzing meal or workout…", "正在分析饮食或运动…")
        put("Analyzing your entry…", "正在分析记录…")
        put("Calories remaining", "剩余可摄入热量")
        put("Over target", "超出目标")
        put("kcal left", "千卡剩余")
        put("left", "剩余")
        put("eaten", "已摄入")
        put("Exercise", "运动消耗")
        put("This week", "本周")
        put("Calories this week", "本周热量趋势")
        put("Tap a bar for macros · selects that day's log", "点击柱状图查看营养素并切换至当天记录")
        put("Edit meal / foods", "编辑餐食 / 食物")
        put("View / edit exercises", "查看 / 编辑训练动作")
        put("Clone item", "复制记录")
        put("Remove from log", "删除此条记录")
        put("Clone to…", "复制到…")
        put("Today's log", "今日记录")
        put("Pick a date…", "选择日期…")
        put("Save as meal preset", "保存为套餐预设")
        put("Save to food library", "保存到食物库")
        put("Saved to food library", "已保存到食物库")
        put("Edit food", "编辑食物")
        put("Edit workout", "编辑训练")
        put("No entries yet", "暂无记录")
        put("Tap Log to add food or a workout.", "点击下方「记录」添加饮食或训练。")
        put("No entries on this day", "当天暂无记录")
        put("Use Log and pick this date to backfill an entry.", "点击「记录」并选择该日期即可补录。")
        put("CALORIES EATEN", "已摄入热量")
        put("EAT UP TO", "还可摄入")
        put("OVER TARGET", "超出目标")
        put("ON TARGET", "已达标")
        put("DAILY TARGET", "每日目标")
        put("PROTEIN", "蛋白质")
        put("CARBS", "碳水")
        put("FATS", "脂肪")
        put("Protein", "蛋白质")
        put("Carbs", "碳水")
        put("Fats", "脂肪")
        put("Calories", "热量")
        put("Calories eaten", "已摄入热量")
        put("Calories burned", "消耗热量")
        put("Net calories", "净热量")
        put("Remaining", "剩余")
        put("Target", "目标")
        put("Eaten", "已吃")
        put("Burned", "消耗")
        put("MEALS LOGGED", "已记饮食")
        put("ACTIVITY BURNED", "运动消耗")
        put("Today's Log", "今日记录")
        put("Meals", "饮食")
        put("Activity", "运动")
        put("Workouts", "训练")
        put("No meals logged yet", "还没有饮食记录")
        put("Tap + Log to snap a meal photo, scan a barcode, or describe what you ate.", "点击下方「+ 记录」拍照识别餐食、扫描条形码或输入你吃了什么。")
        put("No activity logged yet", "还没有运动记录")
        put("Log a workout or describe your activity to track calories burned.", "记录一次训练或描述你的运动，追踪消耗的热量。")
        put("No logs for this day", "当天没有记录")
        put("No meals logged on this day", "当天未记录饮食")
        put("No activity logged on this day", "当天未记录运动")
        put("Clone to today", "复制到今天")
        put("Save as preset", "存为预设")
        put("Save food", "保存食物")
        put("Log food", "记录食物")
        put("Edit entry", "编辑记录")
        put("Delete entry", "删除记录")
        put("Jump to today", "回到今天")
        put("Select date", "选择日期")
        put("Log date", "记录日期")
        put("AI Offline", "AI 离线")
        put("AI Online", "AI 在线")

        // --- Week / 7-Day History ---
        put("7-Day History", "7天历史记录")
        put("Weekly Summary", "本周汇总")
        put("Avg Calories", "平均热量")
        put("Avg Protein", "平均蛋白质")
        put("Total Burned", "总消耗")
        put("Days Logged", "已记录天数")
        put("Tap a day to view or edit its logs", "点击某一天查看或编辑当天记录")

        // --- Analytics / Progress Screen ---
        put("Weekly", "按周")
        put("Monthly", "按月")
        put("Last 7 days", "最近 7 天")
        put("Last 30 days", "最近 30 天")
        put("Calories Eaten vs Target", "摄入热量 vs 每日目标")
        put("Tap a bar to see that day's calorie & macro breakdown", "点击柱状图查看当天的热量与三大营养素详情")
        put("Tap a point to see that day's calorie & macro breakdown", "点击数据点查看当天的热量与三大营养素详情")
        put("Target line", "目标线")
        put("Body Composition", "身体成分趋势")
        put("Tap a point to inspect that reading", "点击数据点查看当次测量详情")
        put("Log at least 1 body measurement in Profile to see trends.", "在「身体」页面记录至少 1 次身体测量数据后即可查看趋势。")
        put("Weight (kg)", "体重 (kg)")
        put("Body fat (%)", "体脂率 (%)")
        put("Muscle (kg)", "肌肉量 (kg)")
        put("Calories Burned · Last 6 months", "运动消耗热量 · 最近 6 个月")
        put("Weekly totals · tap a point for that week's burned kcal", "每周总计 · 点击数据点查看当周消耗热量")
        put("AI Progress Coach", "AI 进度教练")
        put("Generate insight", "生成 AI 洞察")
        put("Crafting your insight…", "正在生成你的专属洞察…")
        put("Tap Generate insight for a personalised review of your trends.", "点击「生成 AI 洞察」获取针对你近期趋势的个性化分析。")
        put("Nutrition", "营养")
        put("Body Metrics", "身体指标")
        put("7D", "7天")
        put("14D", "14天")
        put("30D", "30天")
        put("90D", "90天")
        put("1Y", "1年")
        put("ALL", "全部")
        put("AI Progress Insight", "AI 趋势洞察")
        put("Generate Insight", "生成洞察")
        put("Regenerate", "重新生成")
        put("Ask Coach", "咨询教练")
        put("Ask follow-up", "继续追问")
        put("Progress Coach", "AI 进度教练")
        put("Clear chat", "清空对话")
        put("Ask about your nutrition, workouts, or body trends…", "询问关于你的营养、训练或身体趋势的问题…")
        put("Send", "发送")
        put("Thinking…", "思考中…")
        put("Analyzing your trends…", "正在分析你的趋势数据…")
        put("Daily Calorie Intake", "每日热量摄入")
        put("Calorie Intake", "热量摄入")
        put("Macro Split", "三大营养素占比")
        put("Daily Macros", "每日营养素")
        put("Workout Calories Burned", "运动热量消耗")
        put("Workout Duration", "运动时长")
        put("Weight Trend", "体重趋势")
        put("Body Fat %", "体脂率 %")
        put("Body Fat", "体脂率")
        put("Muscle Mass", "肌肉量")
        put("BMR", "基础代谢 (BMR)")
        put("No data in this range", "该时间段暂无数据")
        put("Recommendations", "建议")
        put("Body score", "身体评分")

        // --- Body / Profile Screen ---
        put("Body & goals", "身体与目标")
        put("Food library", "食物与套餐库")
        put("Body composition", "身体指标记录")
        put("Log a new reading", "记录新测量数据")
        put("Body fat %", "体脂率 %")
        put("BMR (kcal)", "基础代谢 (kcal)")
        put("Muscle mass (kg)", "肌肉量 (kg)")
        put("Log reading", "保存测量")
        put("Daily targets", "每日营养目标")
        put("Goal mode", "目标模式")
        put("Choose automatically", "自动选择（AI 推荐）")
        put("Body recomposition", "增肌减脂（身体塑形）")
        put("Target weight (kg, optional)", "目标体重（kg，选填）")
        put("Leave blank to let AI suggest a healthy target", "留空则由 AI 建议健康的目标体重")
        put("Recalculate targets with AI", "使用 AI 重新计算目标")
        put("Save manual targets", "保存手动目标")
        put("Daily calories", "每日热量 (kcal)")
        put("Body basics", "基本身体资料")
        put("Age, height, sex, and activity live in Settings → Profile.", "年龄、身高、性别与日常活动量可在「设置 → 个人资料」中修改。")
        put("Reading history", "历史测量记录")
        put("Saved meals", "预设套餐")
        put("Saved foods", "常用食物")
        put("Create meal", "创建套餐")
        put("Create food", "创建食物")
        put("No saved meals yet", "暂无预设套餐")
        put("Build a multi-food meal once (e.g. breakfast plate) and log it in one tap.", "预先组合包含多种食物的一餐（如早餐拼盘），之后即可一键记录。")
        put("Build your first meal", "创建第一个套餐")
        put("No saved foods yet", "暂无常用食物")
        put("Save single dishes or scanned items here for quick logging and meal building.", "将单品菜肴或扫码商品保存在此，方便快速记录或组合套餐。")
        put("Add your first food", "添加第一个常用食物")
        put("Log to today", "记录到今天")
        put("Body & Targets", "身体与目标")
        put("Body Profile", "身体档案")
        put("Daily Targets", "每日营养目标")
        put("Target Weight (kg)", "目标体重 (kg)")
        put("Target weight (kg)", "目标体重 (kg)")
        put("Height (cm)", "身高 (cm)")
        put("Age", "年龄")
        put("Sex", "性别")
        put("Male", "男")
        put("Female", "女")
        put("Other / Prefer not to say", "其他 / 保密")
        put("Prefer not to say", "保密")
        put("Goal", "目标方向")
        put("Fat Loss", "减脂")
        put("Lose Fat", "减脂")
        put("Lose weight", "减脂 / 减重")
        put("Cut", "减脂")
        put("Body Recomposition", "增肌减脂 (塑形)")
        put("Recomp", "增肌减脂")
        put("Maintain", "保持体重")
        put("Maintenance", "保持")
        put("Muscle Gain", "增肌")
        put("Lean Bulk", "干净增肌")
        put("Gain muscle", "增肌")
        put("Bulk", "增肌")
        put("Auto (AI recommended)", "自动（AI 推荐）")
        put("Activity Level", "日常活动量")
        put("Activity level", "日常活动量")
        put("Sedentary", "久坐少动")
        put("Lightly Active", "轻度活动")
        put("Lightly active", "轻度活动")
        put("Moderately Active", "中度活动")
        put("Moderately active", "中度活动")
        put("Very Active", "高度活动")
        put("Very active", "高度活动")
        put("Extra Active", "极高强度")
        put("Extra active", "极高强度")
        put("Desk job, little or no exercise", "办公室工作，几乎不运动")
        put("Light exercise 1–3 days/week", "每周轻度运动 1–3 天")
        put("Moderate exercise 3–5 days/week", "每周中等强度运动 3–5 天")
        put("Hard exercise 6–7 days/week", "每周高强度运动 6–7 天")
        put("Very hard training or physical job", "每天高强度训练或体力劳动")
        put("Save Body Profile", "保存身体档案")
        put("Save Profile", "保存个人资料")
        put("Recalculate with AI", "AI 重新计算目标")
        put("Calculate with AI", "AI 计算目标")
        put("Designing targets…", "正在计算营养目标…")
        put("Save Daily Targets", "保存每日目标")
        put("Save Targets", "保存目标")
        put("Daily Calories (kcal)", "每日热量 (kcal)")
        put("Protein (g)", "蛋白质 (g)")
        put("Carbs (g)", "碳水 (g)")
        put("Fats (g)", "脂肪 (g)")
        put("Measurement History", "历史测量记录")
        put("Recent Measurements", "最近测量记录")
        put("No measurements logged yet", "暂无测量记录")
        put("Latest Reading", "最新读数")
        put("Why these targets?", "目标制定依据")

        // --- Log Hub Sheet & Food/Meal Logging ---
        put("Describe", "文字描述")
        put("Snap a meal", "拍照记餐")
        put("From photos", "相册选图")
        put("Type it loosely", "自然语言输入")
        put("Scan barcode", "扫描条码")
        put("Packaged food", "包装食品")
        put("Saved meal", "预设套餐")
        put("One tap", "一键记录")
        put("Saved food", "常用食物")
        put("From library", "从食物库选择")
        put("Gym or cardio", "力量或有氧")
        put("Food", "食物")
        put("Describe what you ate", "描述你吃了什么")
        put("Describe your workout", "描述你的运动")
        put("AI estimates calories and macros from a quick note.", "只需简单描述，AI 即可估算热量与营养素。")
        put("AI estimates calories burned from your activity and body weight.", "AI 会结合你的活动内容与体重估算消耗的热量。")
        put("What did you eat?", "吃了什么？")
        put("Activity (e.g. 5 km run, leg day)", "运动内容（如：5 公里跑、练腿）")
        put("Duration in minutes (optional)", "运动时长（分钟，选填）")
        put("Estimate & log", "AI 估算并记录")
        put("Log Meal or Activity", "记录饮食或运动")
        put("What did you eat or do?", "你吃了什么，或者做了什么运动？")
        put("Snap a photo, scan a barcode, or describe what you ate or how you trained.", "拍照识别、扫描条形码，或直接输入你的饮食与训练内容。")
        put("Camera", "拍照")
        put("Take Photo", "拍照")
        put("Gallery", "相册")
        put("Photo", "照片")
        put("Barcode", "条形码")
        put("Scan Barcode", "扫码")
        put("Saved Foods", "常用食物")
        put("Saved", "已存食物")
        put("Meal Presets", "套餐预设")
        put("Presets", "预设套餐")
        put("Build Meal", "组合餐食")
        put("Log Workout", "记录训练")
        put("Workout", "训练")
        put("Analyze", "AI 分析")
        put("Analyze & Log", "分析并记录")
        put("Add context (optional)", "补充说明（选填）")
        put("Remove photo", "移除照片")

        // --- Region Pack Hints ---
        put("e.g. 2 rotis with dal and a bowl of curd, or 30 min run", "例如：两碗米饭配番茄炒蛋，或跑步 30 分钟")
        put("e.g. 2 eggs, sourdough toast with avocado, or 30 min run", "例如：两个鸡蛋、全麦吐司配牛油果，或跑步 30 分钟")
        put("e.g. quark with berries, wholegrain bread, or 30 min cycling", "例如：酸奶配莓果、全麦面包，或骑行 30 分钟")
        put("e.g. 3 chicken tacos with beans and rice, or 30 min run", "例如：鸡肉卷配米饭黑豆，或跑步 30 分钟")

        // --- Food & Meal Review / Builder ---
        put("Review food", "确认食物")
        put("Review Food", "确认食物")
        put("Review meal", "确认餐食")
        put("Review Meal", "确认餐食")
        put("Edit meal", "编辑餐食")
        put("Edit Meal", "编辑餐食")
        put("Update meal", "更新餐食")
        put("Save meal", "保存餐食")
        put("Foods", "食物列表")
        put("No foods in this meal. Go back and add at least one food.", "该餐食中还没有食物，请返回并至少添加一项食物。")
        put("Total weight (g)", "总重量 (g)")
        put("Change this to scale every ingredient's weight (and macros) proportionally.", "修改总重量将按比例缩放所有配料的重量与营养素。")
        put("Know the dish but not grams? Estimates a standard home serving.", "知道菜名但不知道克数？可让 AI 估算标准家常份量。")
        put("Build a Meal", "组合一餐")
        put("Dish Name", "食物名称")
        put("Dish name", "食物名称")
        put("Meal Name", "餐食名称")
        put("Meal name", "餐食名称")
        put("Ingredients", "配料明细")
        put("Add Ingredient", "添加配料")
        put("Add ingredient", "添加配料")
        put("Add Food", "添加食物")
        put("Add food", "添加食物")
        put("Grams", "克数 (g)")
        put("Weight (g)", "重量 (g)")
        put("Servings", "份数")
        put("Serving size", "单份重量")
        put("Re-analyze", "重新分析")
        put("Re-analyze with AI", "使用 AI 重新分析")
        put("Estimate standard portion", "估算标准份量")
        put("Log Food", "记录食物")
        put("Log Meal", "记录餐食")
        put("Save Changes", "保存修改")
        put("Saved to presets", "已保存到预设")
        put("Saved to foods", "已保存到常用食物")
        put("No meal presets yet", "暂无套餐预设")
        put("Edit Saved Food", "编辑常用食物")
        put("Scanned Product", "扫码商品")
        put("Scan food barcode", "扫描食品条形码")
        put("Point your camera at a nutrition barcode", "将摄像头对准食品包装上的条形码")
        put("Looking up product…", "正在查询商品营养信息…")
        put("Product not found", "未找到该商品信息")

        // --- Workout Log & Exercise Picker ---
        put("Edit Workout", "编辑训练")
        put("Clone Workout", "复制训练")
        put("Workout Name", "训练名称")
        put("Session name", "训练名称")
        put("Session duration (minutes)", "训练时长（分钟）")
        put("Used to estimate calories burned — not a substitute for logging reps.", "用于估算训练消耗热量。")
        put("Estimating calories…", "正在估算消耗热量…")
        put("Save & Estimate Calories", "保存并估算消耗热量")
        put("Suggest name with AI", "AI 智能命名")
        put("Duration (min)", "时长 (分钟)")
        put("Duration", "时长")
        put("Exercises", "训练动作")
        put("Add Exercise", "添加动作")
        put("Add exercise", "添加动作")
        put("Pick Exercise", "选择动作")
        put("Custom Exercise", "自定义动作")
        put("Search exercises…", "搜索训练动作…")
        put("Sets", "组数")
        put("Reps", "次数")
        put("Weight", "重量")
        put("Distance (km)", "距离 (km)")
        put("Save Workout", "保存训练")
        put("Body Part", "部位")
        put("Equipment", "器械")
        put("Chest", "胸部")
        put("Back", "背部")
        put("Shoulders", "肩部")
        put("Upper Arms", "上臂")
        put("Lower Arms", "前臂")
        put("Upper Legs", "大腿")
        put("Lower Legs", "小腿")
        put("Waist", "腰腹 / 核心")
        put("Cardio", "有氧")
        put("Neck", "颈部")
        put("Full Body", "全身")
        put("Barbell", "杠铃")
        put("Dumbbell", "哑铃")
        put("Machine", "固定器械")
        put("Cable", "绳索")
        put("Bodyweight", "自重")
        put("Body Weight", "自重")
        put("Kettlebell", "壶铃")
        put("Band", "弹力带")
        put("Smith Machine", "史密斯机")
        put("EZ Barbell", "曲杆杠铃")
        put("Other", "其他")

        // --- Settings Screen ---
        put("Updates & support", "更新与支持")
        put("Supporting FitBuddy", "支持 FitBuddy")
        put("Check for Update", "检查更新")
        put("Checking for updates…", "正在检查更新…")
        put("Automatically check on startup", "启动时自动检查更新")
        put("Checks GitHub Releases at most once every 24h", "每 24 小时最多检查一次 GitHub Releases")
        put("Send anonymous crash reports", "发送匿名崩溃报告")
        put("Helps fix bugs via Sentry. No personal or meal data is sent.", "通过 Sentry 帮助修复问题，不会发送任何个人或饮食数据。")
        put("Food & diet region", "饮食习惯区域")
        put("Tunes AI portion defaults, barcode fallbacks, and offline staple foods.", "调整 AI 份量估算默认值、条码回退库与离线常见食物。")
        put("Daily log reminder", "每日记录提醒")
        put("Nudge me if I haven't logged a meal today", "若当天尚未记录饮食，向我发送提醒通知")
        put("Reminder time", "提醒时间")
        put("Change", "修改")
        put("Fires only when today's food log is still empty.", "仅在当天饮食记录为空时触发提醒。")
        put("Themes", "主题外观")
        put("Customise colors, fonts, and loading animations.", "自定义配色方案、字体与加载动画。")
        put("Color theme", "色彩主题")
        put("Use dynamic system colors (Material You)", "使用系统动态配色 (Material You)")
        put("Overrides the theme above with your wallpaper palette (Android 12+)", "使用系统壁纸调色板覆盖上方主题（Android 12+）")
        put("Font", "字体")
        put("Component style", "组件风格")
        put("Loading animations", "加载动画")
        put("Analyzing meal / workout", "分析饮食 / 运动动画")
        put("Shown on the home banner while AI is logging an entry.", "当 AI 正在解析记录时在首页顶部横幅展示。")
        put("Generating AI insight", "生成 AI 洞察动画")
        put("Shown inside the Progress tab's AI Progress Coach card.", "在「趋势」页面的 AI 进度教练卡片中展示。")
        put("UI animations", "界面动画")
        put("Smooth progress bars, calorie ring, and chart transitions", "启用平滑的进度条、热量环与图表过渡动画")
        put("AI Provider", "AI 服务商")
        put("FitBuddy works with OpenRouter, Google Gemini, Ollama (local or Ollama Cloud), or any OpenAI-compatible API.", "FitBuddy 支持 OpenRouter、Google Gemini、Ollama（本地或云端）以及任何兼容 OpenAI 的接口。")
        put("Need help getting an API key?", "需要获取 API 密钥的帮助？")
        put("Provider", "服务商")
        put("Configure AI, appearance, backups, and app preferences.", "配置 AI 模型、外观主题、数据备份与应用偏好（含多语言）。")
        put("AI Configuration", "AI 配置")
        put("Platform", "平台")
        put("API Key", "API 密钥")
        put("API Keys", "API 密钥")
        put("Add API Key", "添加密钥")
        put("Paste API key", "粘贴 API 密钥")
        put("Photo / Vision Model", "图片 / 视觉模型")
        put("Vision Model", "视觉模型")
        put("Text Model", "文本模型")
        put("Text Model (optional)", "文本模型（选填）")
        put("Auto failover", "自动故障切换")
        put("Show paid models", "显示付费模型")
        put("Refresh models", "刷新模型列表")
        put("Save AI Settings", "保存 AI 设置")
        put("Save AI settings", "保存 AI 设置")
        put("Connect with OpenRouter", "连接 OpenRouter")
        put("Continue with OpenRouter", "通过 OpenRouter 登录")
        put("Disconnect", "断开连接")
        put("Connected via OAuth", "已通过 OAuth 连接")
        put("Local Server", "本地服务器")
        put("Ollama Cloud", "Ollama 云端")
        put("Server URL", "服务器地址")
        put("Base URL", "接口地址 (Base URL)")

        put("Profile", "个人资料")
        put("Permanent details used for BMR and calorie calculations.", "用于计算基础代谢 (BMR) 与热量目标的基本资料。")
        put("Save profile", "保存个人资料")

        put("Themes & Appearance", "主题与外观")
        put("Appearance", "外观")
        put("Material You (Dynamic color)", "Material You 动态取色")
        put("Material You", "Material You 动态配色")
        put("Uses your wallpaper colors on Android 12+.", "在 Android 12+ 上跟随系统壁纸自动配色。")
        put("Color Theme", "色彩主题")
        put("Color palette", "配色方案")
        put("App font", "应用字体")
        put("Component Style", "组件风格")
        put("Material", "Material 圆润")
        put("Mako", "Mako 硬朗")
        put("Loading Animations", "加载动画")
        put("Analyzing banner animation", "分析横幅动画")
        put("Insight button animation", "洞察按钮动画")
        put("Random", "随机")
        put("Off", "关闭")

        put("Preferences", "偏好设置")
        put("India", "印度")
        put("North America (US & Canada)", "北美（美国与加拿大）")
        put("Europe", "欧洲")
        put("Latin America", "拉丁美洲")
        put("Day rollover hour", "日期切换时间（跨天小时）")
        put("Day change hour", "跨天时间")
        put("Midnight (12:00 AM)", "午夜 (00:00)")
        put("Auto-check for updates", "自动检查更新")
        put("Check for updates on startup", "启动时自动检查新版本")

        put("Backup", "数据备份")
        put("Backup & Restore", "备份与恢复")
        put("Cloud Backup", "云端备份")
        put("Cloud backup", "云端备份")
        put("Local Backup", "本地备份")
        put("Local backup", "本地备份")
        put("Auto-upload every 12h", "每 12 小时自动上传")
        put("Upload now", "立即上传")
        put("Restore from cloud", "从云端恢复")
        put("Change password", "修改密码")
        put("Export backup", "导出备份")
        put("Import backup", "导入备份")
        put("Export JSON", "导出备份文件")
        put("Import JSON", "导入备份文件")

        put("Privacy & Diagnostics", "隐私与诊断")
        put("Send crash reports", "发送崩溃报告")
        put("Diagnostic logging", "诊断日志")
        put("Export diagnostic log", "导出诊断日志")
        put("Clear diagnostic log", "清空诊断日志")
        put("Support ID", "支持 ID (Support ID)")
        put("Copy Support ID", "复制支持 ID")
        put("Regenerate Support ID", "重新生成支持 ID")

        put("Support FitBuddy", "支持 FitBuddy")
        put("Donate", "捐赠支持")
        put("Supporters Hall of Fame", "支持者名人堂")
        put("View Supporters", "查看支持者")
        put("About", "关于")
        put("Version", "版本")
        put("Check for updates", "检查更新")
        put("Up to date", "已是最新版本")
        put("Source code on GitHub", "GitHub 开源仓库")
        put("Report an issue", "反馈问题")
        put("Developer", "开发者选项")
        put("Developer Tools", "开发者工具")

        // --- Onboarding & Opt-in Screens ---
        put("Welcome to FitBuddy", "欢迎使用 FitBuddy")
        put("AI-powered nutrition & workout tracker", "AI 驱动的饮食与运动健康助手")
        put("Get Started", "开始使用")
        put("Restore from backup", "从备份恢复")
        put("Restore from cloud backup", "从云端备份恢复")
        put("Restore from local file", "从本地文件恢复")
        put("Choose your region", "选择你的饮食区域")
        put("Choose your food region", "选择你的饮食区域")
        put("We use this to personalise calorie estimates, staple foods, and coaching tone for your diet and lifestyle.", "我们会根据你所在的区域个性化估算热量、常见主食以及 AI 教练建议风格。")
        put("Don't see your region? Request another", "没有看到你的区域？申请添加其他区域")
        put("Request another region", "申请添加其他区域")
        put("Tell us which country or region's diet to add next.", "告诉我们你希望接下来支持哪个国家或地区的饮食习惯。")
        put("Region or country", "国家或地区")
        put("Send region request?", "发送区域申请？")
        put("No meals, photos, names, or API keys are ever included.", "绝不会包含任何餐食、照片、姓名或 API 密钥。")
        put("Cloud Backup (Optional)", "云端备份（可选）")
        put("Crash Reporting", "崩溃报告")
        put("Finish Setup", "完成设置")
        put("Validate & Continue", "验证并继续")
        put("How to create an OpenRouter API key", "如何创建 OpenRouter API 密钥")
        put("How to create a Gemini API key", "如何创建 Gemini API 密钥")
        put("Ollama install & setup guide", "Ollama 安装与配置指南")
        put("OpenAI API keys & compatible APIs", "OpenAI API 密钥与兼容接口指南")

        // --- Loading Animation Captions ---
        put("ANALYZING", "分析中")
        put("reading the plate…", "正在识别餐盘食物…")
        put("estimating portions…", "正在估算份量…")
        put("crunching macros…", "正在计算营养素…")
        put("checking micronutrients…", "正在核对营养成分…")
        put("almost ready…", "即将完成…")

        // --- Common Status & Toast Messages ---
        put("Settings saved", "设置已保存")
        put("Profile saved", "个人资料已保存")
        put("Body saved · dashboard recalibrated", "身体档案已保存 · 首页已重新校准")
        put("Manual targets saved · dashboard recalibrated", "手动目标已保存 · 首页已重新校准")
        put("Your science-based targets are ready", "基于科学的营养目标已就绪")
        put("Calculating your science-based targets…", "正在计算科学的营养目标…")
        put("Using default targets — refine anytime in Body", "已使用默认目标 — 可随时在「身体」页面微调")
        put("Notifications not allowed.", "未授予通知权限。")
        put("Copied to clipboard", "已复制到剪贴板")
        put("Support ID copied", "已复制支持 ID")
        put("Cloud backup already up to date", "云端备份已是最新")
        put("Cloud backup password updated", "云端备份密码已更新")
        put("Model cooldowns cleared", "模型冷却状态已清除")
        put("Built-in model defaults applied", "已恢复内置默认模型")
    }

    private val loggedFoodRegex = Regex("""^Logged (.+) · (\d+) kcal$""")
    private val updatedFoodRegex = Regex("""^Updated (.+) · (\d+) kcal$""")
    private val loggedWorkoutRegex = Regex("""^Logged (.+) · (\d+) kcal burned$""")
    private val updatedWorkoutRegex = Regex("""^Updated (.+) · (\d+) kcal burned$""")
    private val removedItemRegex = Regex("""^Removed (.+)$""")
    private val savedToFoodLibraryRegex = Regex("""^Saved "(.+)" to food library$""")
    private val savedAsMealPresetRegex = Regex("""^Saved "(.+)" as meal preset$""")
    private val importedRecordsRegex = Regex("""^Imported (\d+) records$""")
    private val restoredCloudRegex = Regex("""^Restored (\d+) records from cloud backup$""")
    private val uploadedCloudRegex = Regex("""^Uploaded (\d+) records to cloud backup$""")
    private val sharingRecordsRegex = Regex("""^Sharing (\d+) records…$""")
    private val greetingNameRegex = Regex("""^(Good morning|Good afternoon|Good evening),\s*(.+)$""")
    private val kcalRemainingRegex = Regex("""^(\d+)\s*kcal\s+left$""", RegexOption.IGNORE_CASE)
    private val kcalOverRegex = Regex("""^(\d+)\s*kcal\s+over$""", RegexOption.IGNORE_CASE)
    private val setsRepsRegex = Regex("""^(\d+)\s*sets\s*×\s*(\d+)\s*reps$""", RegexOption.IGNORE_CASE)
    private val minBurnedRegex = Regex("""^(\d+)\s*min\s*·\s*(\d+)\s*kcal$""")
    private val versionRegex = Regex("""^Version\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val activeModelRegex = Regex("""^Active:\s*(.+)$""")
    private val stepOfRegex = Regex("""^Step\s+(\d+)\s+of\s+(\d+)$""", RegexOption.IGNORE_CASE)
    private val foodsInMealRegex = Regex("""^(\d+)\s+food\(s\)\s+in\s+this\s+meal$""")
    private val servingsWeightIngRegex = Regex("""^([0-9.]+)\s+serving\(s\)\s*·\s*(\d+)\s*g\s*·\s*(\d+)\s+ingredient\(s\)$""")
    private val avgBurnedPerDayRegex = Regex("""^Avg\s+(\d+)\s*kcal\s+burned\s*/\s*day\s*·\s*target\s+adjusted\s+to\s+(\d+)\s*kcal$""")

    private fun translateDynamic(text: String): String {
        greetingNameRegex.matchEntire(text)?.let { m ->
            val greet = translate(m.groupValues[1])
            return "$greet，${m.groupValues[2]}"
        }
        loggedFoodRegex.matchEntire(text)?.let { m ->
            return "已记录 ${m.groupValues[1]} · ${m.groupValues[2]} 千卡"
        }
        updatedFoodRegex.matchEntire(text)?.let { m ->
            return "已更新 ${m.groupValues[1]} · ${m.groupValues[2]} 千卡"
        }
        loggedWorkoutRegex.matchEntire(text)?.let { m ->
            return "已记录 ${m.groupValues[1]} · 消耗 ${m.groupValues[2]} 千卡"
        }
        updatedWorkoutRegex.matchEntire(text)?.let { m ->
            return "已更新 ${m.groupValues[1]} · 消耗 ${m.groupValues[2]} 千卡"
        }
        removedItemRegex.matchEntire(text)?.let { m ->
            return "已移除 ${m.groupValues[1]}"
        }
        savedToFoodLibraryRegex.matchEntire(text)?.let { m ->
            return "已将「${m.groupValues[1]}」保存到食物库"
        }
        savedAsMealPresetRegex.matchEntire(text)?.let { m ->
            return "已将「${m.groupValues[1]}」保存为套餐预设"
        }
        importedRecordsRegex.matchEntire(text)?.let { m ->
            return "已导入 ${m.groupValues[1]} 条记录"
        }
        restoredCloudRegex.matchEntire(text)?.let { m ->
            return "已从云端备份恢复 ${m.groupValues[1]} 条记录"
        }
        uploadedCloudRegex.matchEntire(text)?.let { m ->
            return "已上传 ${m.groupValues[1]} 条记录至云端备份"
        }
        sharingRecordsRegex.matchEntire(text)?.let { m ->
            return "正在分享 ${m.groupValues[1]} 条记录…"
        }
        kcalRemainingRegex.matchEntire(text)?.let { m ->
            return "剩余 ${m.groupValues[1]} kcal"
        }
        kcalOverRegex.matchEntire(text)?.let { m ->
            return "超出 ${m.groupValues[1]} kcal"
        }
        setsRepsRegex.matchEntire(text)?.let { m ->
            return "${m.groupValues[1]} 组 × ${m.groupValues[2]} 次"
        }
        minBurnedRegex.matchEntire(text)?.let { m ->
            return "${m.groupValues[1]} 分钟 · ${m.groupValues[2]} kcal"
        }
        versionRegex.matchEntire(text)?.let { m ->
            return "版本 ${m.groupValues[1]}"
        }
        activeModelRegex.matchEntire(text)?.let { m ->
            return "当前生效：${m.groupValues[1]}"
        }
        stepOfRegex.matchEntire(text)?.let { m ->
            return "第 ${m.groupValues[1]} / ${m.groupValues[2]} 步"
        }
        foodsInMealRegex.matchEntire(text)?.let { m ->
            return "本餐共包含 ${m.groupValues[1]} 项食物"
        }
        servingsWeightIngRegex.matchEntire(text)?.let { m ->
            return "${m.groupValues[1]} 份 · ${m.groupValues[2]} g · ${m.groupValues[3]} 种配料"
        }
        avgBurnedPerDayRegex.matchEntire(text)?.let { m ->
            return "日均消耗 ${m.groupValues[1]} kcal · 每日目标已动态调整为 ${m.groupValues[2]} kcal"
        }
        if (text.startsWith("Export failed: ")) {
            return "导出失败：${text.removePrefix("Export failed: ")}"
        }
        if (text.startsWith("Import failed: ")) {
            return "导入失败：${text.removePrefix("Import failed: ")}"
        }
        if (text.startsWith("Cloud upload failed: ")) {
            return "云端上传失败：${text.removePrefix("Cloud upload failed: ")}"
        }
        return text
    }
}
