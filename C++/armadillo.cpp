#include <armadillo>
#include <iostream>

#include "armadillo.h"
#include "util.h"
template <typename T, typename JavaFormat, typename Transformation>
void jni_to_vec(JNIEnv &env, jobject obj, std::vector<T> & vec, Transformation tr, JavaFormat)
{
    int size = env.GetDirectBufferCapacity(obj);
    JavaFormat *buff = static_cast<JavaFormat*>(env.GetDirectBufferAddress(obj));
    std::transform(buff, buff + size, std::back_inserter(vec), tr);
}

template <class T>
class lshift_inserter_class
{
private:
    T &_vec;
public:
    lshift_inserter_class(T & vec_) : _vec(vec_){}

    inline lshift_inserter_class<T> & operator++(){return *this;}
    inline lshift_inserter_class<T> & operator++(int){return *this;}
    inline lshift_inserter_class<T> & operator*(){ return *this; }
    template <typename U>
    lshift_inserter_class<T> operator=(U const & val) {
        _vec << val;
        return *this;
    }
};

template <class T>
lshift_inserter_class<T> lshift_inserter(T & vec)
{
    return lshift_inserter_class<T>(vec);
}

template <class Iterator>
class pointer_initializer_list_class : public std::initializer_list<typename ITER_UTIL::it_value_type<Iterator>::elem>
{
private:
    Iterator _begin;
    Iterator _end;
public:
    pointer_initializer_list_class(Iterator begin_, Iterator end_)
    {
        _begin(begin_);
        _end(end_);
    }

    Iterator begin(){return _begin;};
    Iterator end(){return _end;};
    size_t size() const{return std::distance(_begin, _end);}
};

template <class Iterator>
pointer_initializer_list_class<Iterator> pointer_initializer_list(Iterator begin, Iterator end)
{
    return pointer_initializer_list<Iterator>(begin, end);
}

template <typename JavaFormat, typename Transformation, typename T>
void jni_to_arma_vec(JNIEnv &env, jobject obj, T & vec, Transformation tr, JavaFormat)
{
    size_t size = env.GetDirectBufferCapacity(obj);
    JavaFormat *buff = static_cast<JavaFormat*>(env.GetDirectBufferAddress(obj));
    for (size_t i = 0; i < size; ++i)
    {
        vec[i] = tr(buff[i]);
    }
    //std::transform(buff, buff + size, lshift_inserter(vec), tr);
    //vec << arma::endr;
}

template <typename T, typename JavaFormat, typename Transformation>
void vec_to_jni(JNIEnv &env, jobject obj, std::vector<T> const & vec, Transformation tr, JavaFormat)
{
    //int size = env.GetDirectBufferCapacity(obj);
    JavaFormat *buff = static_cast<JavaFormat*>(env.GetDirectBufferAddress(obj));
    std::transform(vec.begin(), vec.end(), buff, tr);
}

template <typename T, typename JavaFormat, typename Transformation>
void arma_vec_to_jni(JNIEnv &env, jobject obj, T const & vec, Transformation tr, JavaFormat)
{
    //int size = env.GetDirectBufferCapacity(obj);
    JavaFormat *buff = static_cast<JavaFormat*>(env.GetDirectBufferAddress(obj));
    for (size_t i = 0; i < vec.size(); ++i)
    {
        buff[i] = tr(vec[i]);
    }
}

JNIEXPORT void JNICALL Java_maths_Armadillo_spsolve (JNIEnv *env, jclass, jobject mat_values, jobject IA, jobject JA, jobject javab, jobject result)
{
    /*
    template<class T1, class T2, class T3> arma::SpMat<eT>::SpMat(const arma::Base<long long unsigned int, T1>&, const arma::Base<long long unsigned int, T2>&, const arma::Base<eT, T3>&, arma::uword, arma::uword)
    inline SpMat(const Base<uword,T1>& rowind, const Base<uword,T2>& colptr, const Base<eT,T3>& values, const uword n_rows, const uword n_cols);
    */

    arma::urowvec IA_vec(env->GetDirectBufferCapacity(IA));
    jni_to_arma_vec(*env, IA, IA_vec, UTIL::identity_function, (jint)0);
    arma::urowvec JA_vec(env->GetDirectBufferCapacity(JA));
    jni_to_arma_vec(*env, JA, JA_vec, UTIL::identity_function, (jint)0);
    arma::vec value_vec(env->GetDirectBufferCapacity(mat_values));
    jni_to_arma_vec(*env, mat_values, value_vec, UTIL::identity_function, (jdouble)0);
    //auto iter = UTIL::transformation_iterator(env.GetDirectBufferAddress(mat_values),  UTIL::identity_function);
    //auto iter = static_cast<jdouble*>(env->GetDirectBufferAddress(mat_values));
    //arma::vec value_vec(pointer_initializer_list(iter, iter + env->GetDirectBufferCapacity(mat_values)));
    //jni_to_arma_vec(*env, mat_values, value_vec, UTIL::identity_function, (jdouble)0);
    std::cout << JA_vec.n_elem << ' ' << IA_vec.n_elem << ' ' << value_vec.n_elem << ' ' << env->GetDirectBufferCapacity(result) << ' ' << env->GetDirectBufferCapacity(javab) << std::endl;
    arma::sp_mat A(JA_vec, IA_vec,value_vec, env->GetDirectBufferCapacity(result), env->GetDirectBufferCapacity(javab));

    arma::vec b(env->GetDirectBufferCapacity(javab));
    jni_to_arma_vec(*env, javab, b, UTIL::identity_function, (jdouble)0);
    arma::vec x(env->GetDirectBufferCapacity(result));
    arma::superlu_opts opts;

    //opts.allow_ugly  = true;
    opts.equilibrate = true;
    //std::cout << "x:" << x << std::endl;
    //std::cout << "A:" << A << std::endl;
    //std::cout << "b:" << b << std::endl;

    bool status = arma::spsolve(x, A, b, "superlu", opts);  // solve one system superlu"
    if(status == false)  { std::cout << "no solution" << std::endl; }
    arma_vec_to_jni(*env, result, x, UTIL::identity_function, (jdouble)0);
}

int main()
{
}
