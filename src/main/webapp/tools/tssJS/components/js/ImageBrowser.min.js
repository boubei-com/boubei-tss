;(function (window, factory) {
  
  factory((window.ImageBrowser = {}))
  
  ImageBrowser.init()

}(window, (function (exports) {

const ImageBrowser = function () {

  this.images = []

  this.current = 0

  this.show = false

  this._dom

  this._titleDOM

  this._pagnationDOM

  this.init()
}

ImageBrowser.prototype = {

  constructor: ImageBrowser,

  mount (dom) {
    dom.appendChild(this._dom)
    return this
  },
  
  showOnClick (dom) {
    if (dom == void 0) {
      return
    }

    const imageSwiper = this._dom.getElementsByClassName('image-browser__image-swiper')[0]
    while(imageSwiper.firstChild) imageSwiper.removeChild(imageSwiper.firstChild)

    const images = [...dom.getElementsByTagName('a')].map((link, i) => {
      const url = link.dataset.url || link.getAttribute('href')
      const text = link.innerText
      link.setAttribute('href', 'javascript:void(0);')
      link.dataset.url = url
      link.dataset.index = i
      return { url, text }
    })
    this.insertImages(images)
    this.images = images
    
    const clickHandler = (e) => {
      const event = e || window.event
      const target = event.target || event.srcElement
      if (target.nodeName.toLocaleLowerCase() !== 'a') {
        return
      }
      this.toggleClose()
      
      const current = +target.dataset.index
      this.current = current

      const len = this.images.length
      
      ;[(current - 1 + len) % len, current, (current + 1) % len].forEach(i => {
        const image = this.images[i]
        if (image && image.dom.nodeName.toLocaleLowerCase() === 'img') {
          image.dom.setAttribute('src', image.url)
        }
      })

      imageSwiper.scrollLeft = current * 1000

      this._titleDOM.textContent = this.images[current].text
      this._pagnationDOM.textContent = `${current + 1}/${len}`

      const arrowBtns = this._dom.getElementsByClassName('image-browser__arrow-btn')
      if (current == 0) {
        arrowBtns[0].classList.add('image-browser__btn_disable')
      }

      if (current == len - 1) {
        arrowBtns[1].classList.add('image-browser__btn_disable')
      }
    }

    dom.addEventListener('click', clickHandler, false)
  },

  toggleClose () {
    const dom = this._dom
    if (this.show) {
      this.show = false
      dom.classList.remove('image-browser__show')
      this.current = 0
    }
    else {
      this.show = true
      dom.classList.add('image-browser__show')
    }
  },

  lazyLoadImage (index) {
    const image = this.images[index]
    if (image == void 0) {
      return
    }
    const { url, dom } = image
    
    if (dom.nodeName.toLocaleLowerCase() !== 'img') {
      return
    }

    if (dom.getAttribute('src') !== null) {
      return
    }
    dom.setAttribute('src', url)
  },

  init () {
    const browserWrapper = create('div', 'image-browser')
    browserWrapper.appendChild(this.createToolbar())
    browserWrapper.appendChild(this.createImageNav())

    const pagnation = create('div', 'image-browser__pagnation')
    pagnation.textContent = '0/0'
    this._pagnationDOM = browserWrapper.appendChild(pagnation)

    this._dom = browserWrapper

    this.mount(document.body)

    const ext = document.getElementsByClassName('ext')[0]
    this.showOnClick(ext)
  },
  
  createToolbar () {
    const toolbar = create('div', 'image-browser__row image-browser__toolbar')

    const $1 = create('div', 'image-browser__toolbar-item image-browser__toolbar-item_left')
    const rotateLeftBtn = create('button', 'image-browser__btn image-browser__rotate-btn')
    rotateLeftBtn.textContent = '向左旋转'
    rotateLeftBtn.onclick = () => { rotate(this.images[this.current].dom, -90) }
    $1.appendChild(rotateLeftBtn)
    const rotateRightBtn = create('button', 'image-browser__btn image-browser__rotate-btn')
    rotateRightBtn.textContent = '向右旋转'
    rotateRightBtn.onclick = () => { rotate(this.images[this.current].dom, 90) }
    $1.appendChild(rotateRightBtn)

    const $2 = create('div', 'image-browser__toolbar-item image-browser__toolbar-item_center')
    const title = create('div', 'image-browser__title')
    title.textContent = '文件名'
    this._titleDOM = $2.appendChild(title)
    
    const $3 = create('div', 'image-browser__toolbar-item image-browser__toolbar-item_right')
    const downloadBtn = create('button', 'image-browser__btn image-browser__download-btn')
    downloadBtn.textContent = '下载'
    downloadBtn.onclick = () => {
      const { text, url } = this.images[this.current]
      const xhr = new XMLHttpRequest()
      xhr.open('GET', url)
      xhr.responseType = 'blob'
      xhr.onload = e => {
        const blob = new Blob([xhr.response], { type: xhr.response.type })
        const reader = new FileReader()
        reader.onload = () => {
          const tempLink = create('a')
          tempLink.style.display = 'none'
          this._dom.appendChild(tempLink)
          // const url = URL.createObjectURL(xhr.response)
          tempLink.href = reader.result
          tempLink.download = text
          tempLink.click()
          // URL.revokeObjectURL(url)
          this._dom.removeChild(tempLink)
        }
        reader.readAsDataURL(blob)
      }
      xhr.send()
    }
    $3.appendChild(downloadBtn)
    const openNewWindowBtn = create('button', 'image-browser__btn image-browser__download-btn')
    openNewWindowBtn.textContent = '新窗口中查看'
    openNewWindowBtn.onclick = () => {
      window.open(window.location.origin + this.images[this.current].url,"_blank");
    }
    $3.appendChild(openNewWindowBtn);
    const closeBtn = create('button', 'image-browser__btn image-browser__close-btn ion-ios-close-circle-outline')
    closeBtn.onclick = this.toggleClose.bind(this)
    $3.appendChild(closeBtn);

    [$1, $2, $3].forEach(e => toolbar.appendChild(e))

    return toolbar
  },

  createImageNav () {
    const imageNav = create('div', 'image-browser__row image-browser__image-nav')

    const imageSwiper = create('div', 'image-browser__image-swiper')
    const createNavigatorBtn = arrow => {
      const btn = create('button', `image-browser__btn image-browser__arrow-btn ion-ios-arrow-${arrow}`)
      return btn
    }

    const backBtn = createNavigatorBtn('back')
    const forwardBtn = createNavigatorBtn('forward')

    ;[backBtn, imageSwiper, forwardBtn].forEach(e => imageNav.appendChild(e))

    const switchImage = (step) => {
      return () => {
        const len = this.images.length
        const current = (this.current + step + len) % len
        imageSwiper.scrollLeft = current * 1000
        this._titleDOM.textContent = this.images[current].text
        this._pagnationDOM.textContent = `${current + 1}/${len}`
        this.lazyLoadImage((current + step + len) % len)
        this.current = current
      }
    }

    const previous = switchImage(-1)
    const next = switchImage(1)

    backBtn.onclick = previous
    forwardBtn.onclick = next

    return imageNav
  },

  createImgDOM ({ src, index }) {
    const image = create('img', 'image-browser__image')
    image.setAttribute('alt', src)
    image.style = '-webkit-transform:rotate(0);transform:rotate(0)'
    image.dataset.index = index

    return image
  },

  createUnknownFileTypeDOM (url,index) {
    const div = create('div', 'image-browser__image-placeholder')
    div.innerHTML = '<a href="' + url + '" target="_blank" style="font-size:18px">预览或下载</a>'
    div.dataset.index = index
    return div
  },

  insertImages (images) {
    const imageSwiper = this._dom.getElementsByClassName('image-browser__image-swiper')[0]
    const extensions = ['png', 'jpg', 'jpeg', 'gif', 'webp']
    images.forEach((e, index) => {
      let dom = extensions.includes(getFileExtension(e.text).toLocaleLowerCase()) ?
        this.createImgDOM({ src: e.url, index }) :
        this.createUnknownFileTypeDOM(e.url,index)

      const imageWrapper = create('div', 'image-browser__image-wrapper')
      imageWrapper.style.left = `${index * 1000}px`
  
      imageWrapper.appendChild(dom)
      imageSwiper.appendChild(imageWrapper)
  
      e.dom = dom
    })
  }
}

function getFileExtension (filename) {
  return (/[.]/.exec(filename)) ? /[^.]+$/.exec(filename)[0] : void 0
}

function create (tag, className) {
  const dom = document.createElement(tag)
  className && (dom.className = className)
  return dom
}

function rotate (dom, deg) {
  const matrix = window.getComputedStyle(dom, null).getPropertyValue('transform')
  const [a, b] = matrix.split('(')[1].split(')')[0].split(',')
  const _deg = Math.round(Math.atan2(b, a) * (180 / Math.PI))
  dom.style = `-webkit-transform:rotate(${_deg + deg}deg);transform:rotate(${_deg + deg}deg)`
}

function scroll ({ dom, offset }) {
  dom.scrollLeft += offset
}

const init = function () {
  let sharedInstance = null
  return function () {
    return sharedInstance || (sharedInstance = new ImageBrowser())
  }
}()

function showOnClick (dom) {
  init().showOnClick(dom)
}

exports.init = init
exports.showOnClick = showOnClick
})));