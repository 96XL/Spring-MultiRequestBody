package com.xl.multirequestbody.resolver;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.xl.multirequestbody.annotation.MultiRequestBody;
import com.xl.multirequestbody.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodArgumentResolver;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MultiRequestBody 解析器
 *
 * @author XL
 */
public class MultiRequestBodyArgumentResolver extends AbstractMessageConverterMethodArgumentResolver {

    private static final String JSON_REQUEST_BODY = "JSON_REQUEST_BODY";

    private static Set<Class> classSet;

    static {
        classSet = new HashSet<>();
        classSet.add(Byte.class);
        classSet.add(Short.class);
        classSet.add(Integer.class);
        classSet.add(Long.class);
        classSet.add(Float.class);
        classSet.add(Double.class);
        classSet.add(Boolean.class);
        classSet.add(Character.class);
        classSet.add(String.class);
    }

    public MultiRequestBodyArgumentResolver(List<HttpMessageConverter<?>> converters) {
        super(converters);
    }

    /**
     * 设置支持的方法参数类型
     *
     * @param parameter 方法参数
     * @return 支持的类型
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 支持带 @MultiRequestBody 注解的参数
        return parameter.hasParameterAnnotation(MultiRequestBody.class);
    }

    /**
     * 参数解析
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Object arg = readWithMessage(parameter, webRequest);
        String name = parameter.getParameterName();
        if (binderFactory != null) {
            WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
            if (arg != null) {
                this.validateIfApplicable(binder, parameter);
                if (binder.getBindingResult().hasErrors() && this.isBindExceptionRequired(binder, parameter)) {
                    throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
                }
            }

            if (mavContainer != null) {
                mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
            }
        }
        return arg;
    }

    /**
     * 获取参数
     */
    private Object readWithMessage(MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
        // 获取请求 json, 并使用 jackson 解析参数
        String jsonBody = getRequestBody(webRequest);
        // 这里的 Dict 是 Hutool 工具中封装的 map
        Dict dict = JsonUtils.parseMap(jsonBody);

        // 获取参数类型
        Class<?> parameterType = parameter.getParameterType();
        MultiRequestBody parameterAnnotation = parameter.getParameterAnnotation(MultiRequestBody.class);
        boolean isRequired = parameterAnnotation.required();
        // 将注解 value 作为 json 解析的 key, 如果没有设置 value, 则使用参数名当作 key
        String key = StrUtil.isNotEmpty(parameterAnnotation.value()) ? parameterAnnotation.value() : parameter.getParameterName();
        // 获取参数值
        Object value = dict.get(key);

        // 基本数据类型
        if (parameterType.isPrimitive()) {
            // 如果没有解析到值, 无论该参数是否必传都抛出异常
            // 因为基本数据类型不能为 null, 如果不报错就需要返回一个默认值, 可能会对业务产生影响, 所以这里抛出了异常
            if (value == null) {
                throw new IllegalArgumentException(String.format("isRequired param %s is not present", key));
            }
            return parsePrimitive(parameterType.getName(), value);
        }

        // 包装数据类型和字符串类型
        if (classSet.contains(parameterType)) {
            // 如果没有解析到值, 则判断该参数是否必传, 如果必传则抛出异常, 如果非必传则默认为 null
            if (value == null) {
                if (isRequired) {
                    throw new IllegalArgumentException(String.format("isRequired param %s is not present", key));
                } else {
                    return null;
                }
            }
            return parameterType == String.class ? value.toString() : parseBasicTypeWrapper(parameterType, value);
        }

        // 其他数据类型解析到值时的参数处理
        if (value != null) {
            return JsonUtils.parseObject(value, parameterType);
        }

        // 如果其他数据类型没有解析到值, 判断是否允许解析外层属性
        if (!parameterAnnotation.parseAllFields()) {
            // 不允许解析, 如果参数必传则抛出异常
            if (isRequired) {
                throw new IllegalArgumentException(String.format("isRequired param %s is not present", key));
            }
            // 不允许解析, 如果参数非必传则默认为 null
            return null;
        }

        // 允许解析外层属性, 进行解析
        Object result;
        try {
            result = JsonUtils.parseObject(jsonBody, parameterType);
        } catch (Exception e) {
            // TODO 异常处理返回 null 是否合理?
            result = null;
        }

        // 如果是非必传参数直接将外层属性解析结果返回
        if (!isRequired) {
            return result;
        }

        // 如果是必传参数, 需要判断至少有一个属性有值才能返回, 否则报错
        boolean haveValue = false;
        Field[] declaredFields = parameterType.getDeclaredFields();
        for (Field field : declaredFields) {
            field.setAccessible(true);
            if (field.get(result) != null) {
                haveValue = true;
                break;
            }
        }
        if (!haveValue) {
            throw new IllegalArgumentException(String.format("isRequired param %s is not present", key));
        }
        return result;
    }

    /**
     * 获取请求体 json 字符串
     */
    private String getRequestBody(NativeWebRequest webRequest) {
        // 有就直接获取
        String jsonBody = (String) webRequest.getAttribute(JSON_REQUEST_BODY, NativeWebRequest.SCOPE_REQUEST);

        // 没有就从请求中读取
        if (jsonBody == null) {
            try {
                HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
                jsonBody = IoUtil.read(servletRequest.getReader());
                webRequest.setAttribute(JSON_REQUEST_BODY, jsonBody, NativeWebRequest.SCOPE_REQUEST);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return jsonBody;
    }

    /**
     * 基本数据类型解析
     */
    private Object parsePrimitive(String parameterTypeName, Object value) {
        final String byteTypeName = "byte";
        if (byteTypeName.equals(parameterTypeName)) {
            return Byte.valueOf(value.toString());
        }
        final String shortTypeName = "short";
        if (shortTypeName.equals(parameterTypeName)) {
            return Short.valueOf(value.toString());
        }
        final String intTypeName = "int";
        if (intTypeName.equals(parameterTypeName)) {
            return Integer.valueOf(value.toString());
        }
        final String longTypeName = "long";
        if (longTypeName.equals(parameterTypeName)) {
            return Long.valueOf(value.toString());
        }
        final String floatTypeName = "float";
        if (floatTypeName.equals(parameterTypeName)) {
            return Float.valueOf(value.toString());
        }
        final String doubleTypeName = "double";
        if (doubleTypeName.equals(parameterTypeName)) {
            return Double.valueOf(value.toString());
        }
        final String booleanTypeName = "boolean";
        if (booleanTypeName.equals(parameterTypeName)) {
            return Boolean.valueOf(value.toString());
        }
        final String charTypeName = "char";
        if (charTypeName.equals(parameterTypeName)) {
            return value.toString().charAt(0);
        }
        return null;
    }

    /**
     * 包装数据类型解析
     */
    private Object parseBasicTypeWrapper(Class<?> parameterType, Object value) {
        if (Number.class.isAssignableFrom(parameterType)) {
            Number number = (Number) value;
            if (parameterType == Byte.class) {
                return number.byteValue();
            }
            if (parameterType == Short.class) {
                return number.shortValue();
            }
            if (parameterType == Integer.class) {
                return number.intValue();
            }
            if (parameterType == Long.class) {
                return number.longValue();
            }
            if (parameterType == Float.class) {
                return number.floatValue();
            }
            if (parameterType == Double.class) {
                return number.doubleValue();
            }
        }
        if (parameterType == Boolean.class) {
            return value.toString();
        }
        if (parameterType == Character.class) {
            return value.toString().charAt(0);
        }
        return null;
    }
}